package org.com.dungeontalk.domain.aichat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.com.dungeontalk.domain.aichat.dto.AiGameMessageDto;
import org.com.dungeontalk.domain.aichat.dto.request.AiErrorRequest;
import org.com.dungeontalk.domain.aichat.dto.request.AiGenerateRequest;
import org.com.dungeontalk.domain.aichat.dto.request.AiResponseRequest;
import org.com.dungeontalk.domain.aichat.dto.response.AiServiceResponse;
import org.com.dungeontalk.global.rsData.RsData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiGameFlowServiceTest {

    @Mock
    private AiGameMessageService aiGameMessageService;

    @Mock
    private AiGameStateService aiGameStateService;

    @Mock
    private AiApiService aiApiService;

    @InjectMocks
    private AiGameFlowService service;

    private AiGenerateRequest genReq() {
        return AiGenerateRequest.builder()
            .gameId("game-1")
            .currentUser("user-1")
            .currentMessage("hello")
            .turnNumber(3)
            .build();
    }

    private AiResponseRequest resReq() {
        return AiResponseRequest.builder()
            .gameId("game-1")
            .content("AI reply")
            .turnNumber(4)
            .build();
    }

    private AiErrorRequest errReq() {
        return AiErrorRequest.builder()
            .gameId("game-1")
            .turnNumber(5)
            .errorMessage("timeout")
            .build();
    }

    private AiGameMessageDto aiMsg(String content, int turn) {
        return AiGameMessageDto.builder()
            .content(content)
            .turnNumber(turn)
            .build();
    }

    @Nested
    @DisplayName("processAiTurn")
    class ProcessAiTurn {

        @Test
        @DisplayName("락 획득 성공 → 컨텍스트 조회 → AI 호출 → 저장 → unlock & nextTurn → 200")
        void success() {
            String roomId = "room-1";
            AiGenerateRequest request = genReq();

            given(aiGameStateService.lockForAiResponse(roomId)).willReturn(true);
            given(aiGameMessageService.getContextMessages(eq(roomId), anyInt(),
                eq(request.getTurnNumber())))
                .willReturn(List.of(aiMsg("prev", 2)));

            given(aiApiService.generateAiResponse(
                eq(request.getGameId()),
                eq(roomId),
                eq(request.getCurrentUser()),
                eq(request.getCurrentMessage()),
                anyList(),
                eq(request.getTurnNumber())
            )).willReturn(AiServiceResponse.builder()
                .content("AI says hi")
                .responseTime(111L)
                .sources(List.of("s1"))
                .build());

            given(aiGameMessageService.saveAiMessage(any()))
                .willReturn(aiMsg("AI says hi", request.getTurnNumber()));

            given(aiGameStateService.nextTurn(roomId)).willReturn(request.getTurnNumber() + 1);

            // when
            RsData<?> rs = service.processAiTurn(roomId, request);

            // then
            assertThat(rs.getResultCode()).isEqualTo("200");
            assertThat(rs.getMsg()).isEqualTo("AI 응답 생성 및 처리 완료");

            verify(aiGameStateService).unlockAfterAiResponse(roomId);
            verify(aiGameStateService).nextTurn(roomId);
            verify(aiGameMessageService).saveAiMessage(any());
        }

        @Test
        @DisplayName("락 획득 실패 → 400 && 이후의 흐름을 호출하지 않는 경우")
        void lockFailed() {
            String roomId = "room-1";
            AiGenerateRequest request = genReq();

            given(aiGameStateService.lockForAiResponse(roomId)).willReturn(false);

            RsData<?> rs = service.processAiTurn(roomId, request);

            assertThat(rs.getResultCode()).isEqualTo("400");
            assertThat(rs.getMsg()).isEqualTo("AI 응답이 이미 처리 중입니다");

            verify(aiGameMessageService, never()).getContextMessages(anyString(), anyInt(),
                anyInt());
            verify(aiApiService, never()).generateAiResponse(any(), any(), any(), any(), anyList(),
                anyInt());
            verify(aiGameMessageService, never()).saveAiMessage(any());
            verify(aiGameStateService, never()).nextTurn(anyString());
        }

        @Test
        @DisplayName("AI 호출 중 예외 발생 → unlockAfterAiResponse 호출되고 500 반환")
        void aiThrowsException() {
            String roomId = "room-1";
            AiGenerateRequest request = genReq();

            given(aiGameStateService.lockForAiResponse(roomId)).willReturn(true);
            given(aiGameMessageService.getContextMessages(eq(roomId), anyInt(),
                eq(request.getTurnNumber())))
                .willReturn(List.of());
            given(aiApiService.generateAiResponse(any(), any(), any(), any(), anyList(), anyInt()))
                .willThrow(new RuntimeException("boom"));

            RsData<?> rs = service.processAiTurn(roomId, request);

            assertThat(rs.getResultCode()).isEqualTo("500");
            assertThat(rs.getMsg()).isEqualTo("AI 응답 생성 중 오류가 발생했습니다");

            verify(aiGameStateService).unlockAfterAiResponse(roomId);
            verify(aiGameMessageService, never()).saveAiMessage(any());
            verify(aiGameStateService, never()).nextTurn(anyString());
        }
    }

    @Nested
    @DisplayName("handleAiResponse")
    class HandleAiResponse {

        @Test
        @DisplayName("AI 응답 저장 → unlock → nextTurn → 200")
        void success() {
            String roomId = "room-1";
            AiResponseRequest request = resReq();

            given(aiGameMessageService.saveAiMessage(any()))
                .willReturn(aiMsg(request.getContent(), request.getTurnNumber()));

            // 턴 증가
            given(aiGameStateService.nextTurn(roomId)).willReturn(request.getTurnNumber() + 1);

            RsData<?> rs = service.handleAiResponse(roomId, request);

            assertThat(rs.getResultCode()).isEqualTo("200");
            assertThat(rs.getMsg()).isEqualTo("AI 응답 생성 및 처리 완료");

            verify(aiGameStateService).unlockAfterAiResponse(roomId);
            verify(aiGameStateService).nextTurn(roomId);
        }

        @Test
        @DisplayName("저장 중 예외 → unlockAfterAiResponse 호출되고 500 반환")
        void saveThrowException() {
            String roomId = "room-1";
            AiResponseRequest request = resReq();

            given(aiGameMessageService.saveAiMessage(any()))
                .willThrow(new RuntimeException("DB가 죽었습니다"));

            RsData<?> rs = service.handleAiResponse(roomId, request);

            assertThat(rs.getResultCode()).isEqualTo("500");
            assertThat(rs.getMsg()).isEqualTo("AI 응답 처리 중 오류가 발생했습니다");

            verify(aiGameStateService).unlockAfterAiResponse(roomId);
            verify(aiGameStateService, never()).nextTurn(anyString());
        }
    }

    @Nested
    @DisplayName("handleAiError")
    class HandleAiError {

        @Test
        @DisplayName("에러 메시지 시스템 전송 → unlock → pauseGame → 200")
        void success() {
            String roomId = "room-1";
            AiErrorRequest request = errReq();

            given(aiGameMessageService.handleSystemMessage(any()))
                .willReturn(aiMsg("SYSTEM ERROR", request.getTurnNumber()));

            RsData<Void> rs = service.handleAiError(roomId, request);

            assertThat(rs.getResultCode()).isEqualTo("200");
            assertThat(rs.getMsg()).isEqualTo("AI 오류 처리 완료");

            verify(aiGameStateService).unlockAfterAiResponse(roomId);
            verify(aiGameStateService).pauseGame(eq(roomId), contains(request.getErrorMessage()));
        }
    }
}