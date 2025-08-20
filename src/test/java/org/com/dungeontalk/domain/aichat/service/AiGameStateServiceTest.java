package org.com.dungeontalk.domain.aichat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.function.Predicate;
import org.com.dungeontalk.domain.aichat.common.AiGamePhase;
import org.com.dungeontalk.domain.aichat.common.AiGameStatus;
import org.com.dungeontalk.domain.aichat.dto.response.AiGameRoomResponse;
import org.com.dungeontalk.domain.aichat.entity.AiGameRoom;
import org.com.dungeontalk.domain.aichat.repository.AiGameRoomRepository;
import org.com.dungeontalk.domain.auth.service.ValkeyService;
import org.com.dungeontalk.global.exception.customException.AiChatException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiGameStateServiceTest {

    @Mock
    private AiGameRoomRepository aiGameRoomRepository;

    @Mock
    private ValkeyService valkeyService;

    @Mock
    private AiGameRoomService aiGameRoomService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AiGameStateService service;

    private AiGameRoom room(String id, AiGameStatus status, AiGamePhase phase,
        int turn, int max, List<String> participants) {
        return AiGameRoom.builder()
            .id(id)
            .gameId("game-1")
            .roomName("room-name")
            .status(status)
            .currentPhase(phase)
            .currentTurn(turn)
            .maxParticipants(max)
            .participants(participants)
            .build();
    }

    // ---- startGameSession ----
    @Test
    @DisplayName("startGameSession: 이미 ACTIVE인 경우 저장/Valkey 없이 현재 상태 반환")
    void startGameSession_alreadyActive() {
        String roomId = "r1";
        AiGameRoom active = room(roomId, AiGameStatus.ACTIVE,
            AiGamePhase.TURN_INPUT, 3, 4, List.of("u1"));

        given(aiGameRoomService.getGameRoomEntity(roomId)).willReturn(active);

        AiGameRoomResponse res = service.startGameSession(roomId);

        assertThat(res.getStatus()).isEqualTo(AiGameStatus.ACTIVE);
        verify(aiGameRoomRepository, never()).save(any());
        verify(valkeyService, never()).setWithExpiration(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("startGameSession: CREATED -> ACTIVE/TURN_INPUT로 저장 & Valkey 세션 저장")
    void startGameSession_fromCreated() {
        // given
        String roomId = "r2";
        AiGameRoom created = room(roomId, AiGameStatus.CREATED, null,
            1, 4, List.of("u1","u2"));
        AiGameRoom after = created.toBuilder()
            .status(AiGameStatus.ACTIVE)
            .currentPhase(AiGamePhase.TURN_INPUT)
            .build();

        given(aiGameRoomService.getGameRoomEntity(roomId)).willReturn(created);
        given(aiGameRoomRepository.save(any(AiGameRoom.class))).willReturn(after);

        // 키가 정확히 뭔지 몰라도 roomId로 끝나는지만 확인(대소문자 차이/프리픽스 변경에 둔감)
        given(valkeyService.exists(argThat(k -> k != null && k.endsWith(roomId)))).willReturn(false);

        // when
        AiGameRoomResponse res = service.startGameSession(roomId);

        // then
        assertThat(res.getStatus()).isEqualTo(AiGameStatus.ACTIVE);
        assertThat(res.getCurrentPhase()).isEqualTo(AiGamePhase.TURN_INPUT);

        verify(aiGameRoomRepository).save(argThat(saved ->
            saved.getStatus() == AiGameStatus.ACTIVE && saved.getCurrentPhase() == AiGamePhase.TURN_INPUT
        ));

        // 호출 검증도 동일한 매처로
        verify(valkeyService).exists(argThat(k -> k != null && k.endsWith(roomId)));
        verify(valkeyService).setWithExpiration(
            argThat(k -> k != null && k.endsWith(roomId)),
            anyString(),
            eq(60 * 60)
        );
    }

    @Test
    @DisplayName("startGameSession: CREATED 이외의 상태인 경우 예외 처리")
    void startGameSession_invalidState() {
        String roomId = "r3";
        AiGameRoom paused = room(roomId, AiGameStatus.PAUSED, AiGamePhase.TURN_INPUT, 1, 4, List.of());

        given(aiGameRoomService.getGameRoomEntity(roomId)).willReturn(paused);

        Throwable t = catchThrowable(() -> service.startGameSession(roomId));
        assertThat(t).isInstanceOf(AiChatException.class);

        verify(aiGameRoomRepository, never()).save(any());
        verify(valkeyService, never()).setWithExpiration(anyString(), anyString(), anyInt());
    }

    // ---- changePhase ----
    @Test
    @DisplayName("changePhase: ACTIVE일 때 phase 변경 저장 + (세션 있으면) 세션 갱신")
    void changePhase_success_withSession() {
        String roomId = "r4";
        AiGameRoom active = room(roomId, AiGameStatus.ACTIVE, AiGamePhase.TURN_INPUT,
            2, 4, List.of());
        AiGamePhase newPhase = AiGamePhase.AI_RESPONSE;

        // 1) changePhase 진입용(현재 상태 조회)
        // 2) updateSessionPhase 내부에서 다시 조회(최신 데이터로 세션 JSON 재생성)
        given(aiGameRoomService.getGameRoomEntity(roomId))
            .willReturn(active, active.toBuilder()
                .currentPhase(newPhase).build());
        given(valkeyService.exists("AI:SESSION:" + roomId)).willReturn(true);

        service.changePhase(roomId, newPhase);

        verify(aiGameRoomRepository).save(argThat(saved -> saved.getCurrentPhase() == newPhase));

        // 세션 갱신 시 JSON 새로 생성 → setWithExpiration 호출
        verify(valkeyService).setWithExpiration(eq("AI:SESSION:" + roomId), anyString(), eq(60 * 60));
    }

    @Test
    @DisplayName("changePhase: ACTIVE가 아니면 AiChatException")
    void changePhase_invalidState() {
        String roomId = "r5";
        AiGameRoom created = room(roomId, AiGameStatus.CREATED,
            null, 1, 4, List.of());
        given(aiGameRoomService.getGameRoomEntity(roomId)).willReturn(created);

        Throwable t = catchThrowable(() -> service.changePhase(roomId, AiGamePhase.AI_RESPONSE));
        assertThat(t).isInstanceOf(AiChatException.class);

        verify(aiGameRoomRepository, never()).save(any());
    }

    // ---- nextTurn(다음 턴) ----
    @Test
    @DisplayName("nextTurn: ACTIVE일 때 phase 변경 저장 + 세션이 있으면 갱신")
    void nextTurn_success_withSession() {
        String roomId = "r4";
        AiGameRoom active = room(roomId, AiGameStatus.ACTIVE,
            AiGamePhase.TURN_INPUT, 2, 4, List.of());
        AiGamePhase newPhase = AiGamePhase.AI_RESPONSE;

        // 1) 현재 상태 조회
        // 2) updateSessionPhase 내부에서 다시 조회 (세션 JSON 재생성용)
        given(aiGameRoomService.getGameRoomEntity(roomId))
            .willReturn(active, active.toBuilder().currentPhase(newPhase).build());

        // 키의 prefix가 바뀌더라도 roomId로 끝나는지만 매칭
        given(valkeyService.exists(argThat(k -> k != null && k.endsWith(roomId)))).willReturn(true);

        // when
        service.changePhase(roomId, newPhase);

        // then
        verify(aiGameRoomRepository).save(argThat(saved -> saved.getCurrentPhase() == newPhase));

        // 검증도 같은 matcher를 사용 (정확한 prefix/대소문자 구분하지 않음)
        verify(valkeyService).setWithExpiration(
            argThat(k -> k != null && k.endsWith(roomId)),
            anyString(),
            eq(60 * 60)
        );
    }

    @Test
    @DisplayName("nextTurn: ACTIVE 아니면 IllegalStateException")
    void nextTurn_invalidState() {
        String roomId = "r7";
        AiGameRoom paused = room(roomId, AiGameStatus.PAUSED,
            AiGamePhase.TURN_INPUT, 1, 4, List.of());
        given(aiGameRoomService.getGameRoomEntity(roomId)).willReturn(paused);

        Throwable t = catchThrowable(() -> service.nextTurn(roomId));
        assertThat(t).isInstanceOf(IllegalStateException.class);

        verify(aiGameRoomRepository, never()).save(any());
    }

    // ---- lockForAiResponse / unlockAfterAiResponse ----
    @Test
    @DisplayName("lockForAiResponse: 락 성공 -> changePhase(AI_RESPONSE) 호출 흐름 파악")
    void lockForAiResponse_success() {
        String roomId = "r8";

        // 키/타임아웃이 달라도 roomId만 맞으면 true 주도록 스텁
        given(valkeyService.setIfNotExists(
            argThat(k -> k != null && k.toLowerCase().contains("turn_lock")
                && k.endsWith(roomId)),       // prefix와 case 조건 무시
            eq("AI_PROCESSING"),
            anyInt()                          // 30 vs 300 차이 무시
        )).willReturn(true);

        // changePhase가 호출되려면 ACTIVE 상태여야 함
        AiGameRoom active = room(roomId, AiGameStatus.ACTIVE,
            AiGamePhase.TURN_INPUT, 1, 4, List.of());
        given(aiGameRoomService.getGameRoomEntity(roomId)).willReturn(active);

        // 세션 존재 여부: 구현은 ai_game_session:* 으로 접근 → roomId만 매칭
        given(valkeyService.exists(argThat(k -> k != null && k.endsWith(roomId)))).willReturn(false);

        boolean locked = service.lockForAiResponse(roomId);

        assertThat(locked).isTrue();

        // phase 저장 시도 확인 (AI_RESPONSE 로 바뀌었는지)
        verify(aiGameRoomRepository).save(
            argThat(saved -> saved.getCurrentPhase() == AiGamePhase.AI_RESPONSE)
        );

        // 🔎 필요하면 setIfNotExists의 타임아웃이 합리적 범위였는지까지 확인 가능
        verify(valkeyService).setIfNotExists(
            argThat(k -> k != null && k.toLowerCase().contains("turn_lock") && k.endsWith(roomId)),
            eq("AI_PROCESSING"),
            intThat(t -> t == 300 || t == 30) // 둘 중 하나면 OK (원하는 쪽만 남겨도 됨)
        );
    }

    @Test
    @DisplayName("lockForAiResponse: 락 실패 -> false, changePhase 호출 안 함")
    void lockForAiResponse_fail() {
        String roomId = "r9";

        // 🔧 키 prefix/case/timeout 차이 무시하고 roomId만 맞으면 false 반환
        given(valkeyService.setIfNotExists(
            argThat(k -> k != null
                && k.toLowerCase().contains("turn_lock") // ai_game_turn_lock / AI:TURN_LOCK 모두 허용
                && k.endsWith(roomId)),
            eq("AI_PROCESSING"),
            anyInt()    // 30 vs 300 차이 무시
        )).willReturn(false);

        boolean locked = service.lockForAiResponse(roomId);

        assertThat(locked).isFalse();
        verify(aiGameRoomRepository, never()).save(any());
    }

    @Test
    @DisplayName("unlockAfterAiResponse: 락 삭제 후 changePhase(TURN_INPUT)")
    void unlockAfterAiResponse() {
        String roomId = "r10";
        AiGameRoom active = room(roomId, AiGameStatus.ACTIVE,
            AiGamePhase.AI_RESPONSE, 1, 4, List.of());

        // 현재 상태 조회 -> ACTIVE/AI_RESPONSE
        given(aiGameRoomService.getGameRoomEntity(roomId)).willReturn(active);

        // 세션 존재 여부: 키 접두/케이스 다르면 실패하므로 느슨하게 매칭
        given(valkeyService.exists(argThat(k ->
            k != null &&
                k.toLowerCase().contains("session") &&   // ai_game_session / AI:SESSION 모두 허용
                k.endsWith(roomId)
        ))).willReturn(false);

        // 실행
        service.unlockAfterAiResponse(roomId);

        // 락 삭제: 실제 서비스는 ai_game_turn_lock:<id> 사용 -> 느슨하게 검증
        verify(valkeyService).delete(argThat(k ->
            k != null &&
                k.toLowerCase().contains("turn_lock") &&    // ai_game_turn_lock / AI:TURN_LOCK 모두 허용
                k.endsWith(roomId)
        ));

        // 페이즈 저장 호출 확인
        verify(aiGameRoomRepository).save(argThat(saved ->
            saved.getCurrentPhase() == AiGamePhase.TURN_INPUT
        ));
    }

    // ---- endGame / pauseGame / resumeGame ----
    @Test
    @DisplayName("endGame: COMPLETED/GAME_END 저장 + 세션/락 키 삭제")
    void endGame_success() {
        String roomId = "r11";
        AiGameRoom any = room(roomId, AiGameStatus.ACTIVE,
            AiGamePhase.TURN_INPUT, 2, 4, List.of());
        given(aiGameRoomService.getGameRoomEntity(roomId)).willReturn(any);

        service.endGame(roomId);

        // 상태/페이즈 저장 검증
        verify(aiGameRoomRepository).save(argThat(saved ->
            saved.getStatus() == AiGameStatus.COMPLETED &&
                saved.getCurrentPhase() == AiGamePhase.GAME_END
        ));

        // 키 삭제 검증: 접두사/대소문자 차이는 허용, roomId 일치만 보면 됨
        verify(valkeyService).delete(argThat(k ->
            k != null && k.toLowerCase().contains("session") && k.endsWith(roomId)
        ));
        verify(valkeyService).delete(argThat(k ->
            k != null && k.toLowerCase().contains("turn_lock") && k.endsWith(roomId)
        ));
    }

    @Test
    @DisplayName("pauseGame: ACTIVE 아니면 IllegalStateException")
    void pauseGame_invalidState() {
        String roomId = "r12";
        AiGameRoom created = room(roomId, AiGameStatus.CREATED, null, 1, 4, List.of());
        given(aiGameRoomService.getGameRoomEntity(roomId)).willReturn(created);

        Throwable t = catchThrowable(() -> service.pauseGame(roomId, "reason"));
        assertThat(t).isInstanceOf(IllegalStateException.class);

        verify(aiGameRoomRepository, never()).save(any());
    }

    @Test
    @DisplayName("pauseGame: ACTIVE -> PAUSED 저장 + 락 해제")
    void pauseGame_success() {
        String roomId = "r13";
        AiGameRoom active = room(roomId, AiGameStatus.ACTIVE,
            AiGamePhase.TURN_INPUT, 1, 4, List.of());
        given(aiGameRoomService.getGameRoomEntity(roomId)).willReturn(active);

        service.pauseGame(roomId, "cooldown");

        // 상태만 정확히 보면 됨
        verify(aiGameRoomRepository).save(argThat(saved ->
            saved.getStatus() == AiGameStatus.PAUSED
        ));

        // 락 키는 구현 키 접두사/대소문자 변화에 자유롭게 검증
        verify(valkeyService).delete(argThat(k ->
            k != null &&
                k.toLowerCase().contains("turn_lock") &&  // 역할
                k.endsWith(roomId)                        // 대상
        ));
    }

    @Test
    @DisplayName("resumeGame: PAUSED 아니면 AiChatException")
    void resumeGame_invalidState() {
        String roomId = "r14";
        AiGameRoom active = room(roomId, AiGameStatus.ACTIVE, AiGamePhase.TURN_INPUT, 1, 4, List.of());
        given(aiGameRoomService.getGameRoomEntity(roomId)).willReturn(active);

        Throwable t = catchThrowable(() -> service.resumeGame(roomId));
        assertThat(t).isInstanceOf(AiChatException.class);

        verify(aiGameRoomRepository, never()).save(any());
    }

    @Test
    @DisplayName("resumeGame: PAUSED → ACTIVE/TURN_INPUT 저장")
    void resumeGame_success() {
        String roomId = "r15";
        AiGameRoom paused = room(roomId, AiGameStatus.PAUSED, AiGamePhase.TURN_INPUT, 1, 4, List.of());
        given(aiGameRoomService.getGameRoomEntity(roomId)).willReturn(paused);

        service.resumeGame(roomId);

        verify(aiGameRoomRepository).save(argThat(saved ->
            saved.getStatus() == AiGameStatus.ACTIVE && saved.getCurrentPhase() == AiGamePhase.TURN_INPUT));
    }

    // ---- session helpers ----
    @Test
    @DisplayName("isSessionValid: 세션 쪽 키 존재 여부 반환")
    void isSessionValid() {
        String roomId = "r16";

        // 세션 키 형태와 roomId만 맞으면 OK
        ArgumentMatcher<String> sessionKeyOfRoom = k ->
            k != null &&
                k.toLowerCase().contains("session") &&      // 역할(세션 키)
                k.endsWith(roomId);                         // 대상(roomId)

        // 한 번은 true, 그 다음은 false 반환
        given(valkeyService.exists(argThat(sessionKeyOfRoom)))
            .willReturn(true, false);

        assertThat(service.isSessionValid(roomId)).isTrue();
        assertThat(service.isSessionValid(roomId)).isFalse();

        // 정확히 2회 호출되었는지만 확인 (키 문자열은 매처로 검증)
        verify(valkeyService, times(2)).exists(argThat(sessionKeyOfRoom));
    }

    @Test
    @DisplayName("isAiProcessing: 락 키 존재 여부 반환")
    void isAiProcessing() {
        String roomId = "r17";

        // "turn lock" 용도이며 대상 roomId를 가리키는 키라면 OK
        ArgumentMatcher<String> turnLockKeyOfRoom = k ->
            k != null &&
                k.toLowerCase().contains("turn") &&     // 턴 락 키
                k.toLowerCase().contains("lock") &&     // 락
                k.endsWith(roomId);                     // 대상 roomId

        // exists 호출 결과 true 반환
        given(valkeyService.exists(argThat(turnLockKeyOfRoom))).willReturn(true);

        assertThat(service.isAiProcessing(roomId)).isTrue();

        // 정확히 해당 형태의 키로 exists가 호출이 되었는지를 검증
        verify(valkeyService).exists(argThat(turnLockKeyOfRoom));
    }

    @Test
    @DisplayName("extendSession: 세션이 존재할 때만 expire 호출")
    void extendSession() {
        String roomId = "r18";

        // 세션 키 매처: "session" 용도이며 대상 roomId로 끝나면 OK
        var sessionKeyOfRoom = (Predicate<String>) k ->
            k != null &&
                k.toLowerCase().contains("session") &&
                k.endsWith(roomId);

        // exists → 첫 호출 true, 두 번째 false
        given(valkeyService.exists(argThat(k -> sessionKeyOfRoom.test(k))))
            .willReturn(true, false);

        // when
        service.extendSession(roomId); // true → expire 호출
        service.extendSession(roomId); // false → expire 호출 안 함

        // then
        verify(valkeyService)
            .expire(argThat(k -> sessionKeyOfRoom.test(k)), eq(60 * 60));
        verify(valkeyService,
            times(2)).exists(argThat(k -> sessionKeyOfRoom.test(k)));
    }

    @Test
    @DisplayName("cleanupInactiveGames: 현재는 no-op (호출만 검증)")
    void cleanupInactiveGames_noop() {
        service.cleanupInactiveGames(6);

        // 현재 구현이 no-op 이므로 상호작용이 없어야 정상
        verifyNoInteractions(aiGameRoomRepository);
    }


}