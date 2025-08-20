package org.com.dungeontalk.domain.aichat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.aichat.dto.AiGameMessageDto;
import org.com.dungeontalk.domain.aichat.dto.request.*;
import org.com.dungeontalk.domain.aichat.dto.response.AiGameMessageResponse;
import org.com.dungeontalk.domain.aichat.dto.response.AiServiceResponse;
import org.com.dungeontalk.domain.aichat.entity.AiGameRoom;
import org.com.dungeontalk.domain.aichat.event.AiTurnProcessEvent;
import org.com.dungeontalk.domain.gamecharacter.dto.request.GameResultRequest;
import org.com.dungeontalk.domain.gamecharacter.service.GameCharacterService;
import org.com.dungeontalk.global.rsData.RsData;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.com.dungeontalk.domain.aichat.common.AiChatConstants.*;

/**
 * AI 게임 플로우 관리 서비스
 * AI 응답 생성, 처리, 오류 처리 등 AI 게임의 전체 플로우를 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiGameFlowService {

    private final AiGameMessageService aiGameMessageService;
    private final AiGameStateService aiGameStateService;
    private final AiApiService aiApiService;
    // 게임 캐릭터의 경험치/레벨업 로직 처리를 위한 서비스
    private final GameCharacterService gameCharacterService;
    // AI 게임방 정보 조회를 위한 서비스
    private final AiGameRoomService aiGameRoomService;
    // JSON 파싱을 위한 ObjectMapper
    private final ObjectMapper objectMapper;

    /**
     * AI 턴을 처리합니다 (기존 generateAndProcessAiResponse 로직)
     */
    public RsData<AiGameMessageResponse> processAiTurn(String roomId, AiGenerateRequest request) {
        log.info("AI 턴 처리 시작: roomId={}, user={}, turn={}",
                roomId, request.getCurrentUser(), request.getTurnNumber());

        // AI 응답 처리 중으로 락 설정
        boolean locked = aiGameStateService.lockForAiResponse(roomId);
        if (!locked) {
            log.warn("AI 응답 처리 중 락 설정 실패 (이미 처리중): roomId={}", roomId);
            return RsData.of("400", "AI 응답이 이미 처리 중입니다", null);
        }

        try {
            // 컨텍스트 메시지 조회
            List<AiGameMessageDto> contextMessages = aiGameMessageService
                    .getContextMessages(roomId, DEFAULT_CONTEXT_MESSAGE_COUNT, request.getTurnNumber());

            // 게임 시작 시간 계산 (첫 번째 메시지 시간 또는 현재 시간)
            Long gameStartTime = calculateGameStartTime(roomId);

            // Python AI 서비스에서 응답 생성 (시간 관리 및 캐릭터 스탯 포함)
            AiServiceResponse aiResult = aiApiService.generateAiResponse(
                    request.getGameId(),
                    roomId,
                    request.getCurrentUser(),
                    request.getCurrentMessage(),
                    contextMessages,
                    request.getTurnNumber(),
                    gameStartTime,
                    15,  // 15분 목표 시간
                    request.getCharacterStats()  // 캐릭터 스탯 추가
            );

            // AI 메시지 저장
            AiMessageSaveRequest saveRequest = AiMessageSaveRequest.builder()
                    .aiGameRoomId(roomId)
                    .gameId(request.getGameId())
                    .content(aiResult.getContent())
                    .turnNumber(request.getTurnNumber())
                    .build();
            AiGameMessageDto savedMessage = aiGameMessageService.saveAiMessage(saveRequest);

            // AI의 응답 결과, 게임이 종료되었다고 판단하면 후처리 로직 호출
            if (aiResult.isGameEnded()) {
                log.info("🎯 TRPG 게임 자동 종료: gameId={}, roomId={}, gamePhase={}",
                        request.getGameId(), roomId, aiResult.getGamePhase());

                // 게임 종료 및 경험치 지급 처리
                handleGameEnd(roomId, request.getCurrentUser(), aiResult);
            }

            // AI 응답 완료 후 락 해제 및 다음 턴으로 진행 (WebSocket은 saveAiMessage에서 처리됨)
            int nextTurn = completeAiResponseAndProgressToNextTurn(roomId);

            log.info("AI 턴 처리 완료: roomId={}, nextTurn={}, gamePhase={}, remainingTime={}분",
                    roomId, nextTurn, aiResult.getGamePhase(), aiResult.getRemainingTime());

            AiGameMessageResponse response = AiGameMessageResponse.fromDto(savedMessage);
            return RsData.of("200", "AI 응답 생성 및 처리 완료", response);

        } catch (Exception e) {
            return handleAiResponseError(roomId, e, "AI 응답 생성 중 오류가 발생했습니다");
        }
    }

    /**
     * AI 응답을 처리합니다 (기존 receiveAiResponse 로직)
     */
    public RsData<AiGameMessageResponse> handleAiResponse(String roomId, AiResponseRequest request) {
        log.info("AI 응답 처리: roomId={}, turn={}", roomId, request.getTurnNumber());

        try {
            // AI 메시지 저장
            AiMessageSaveRequest saveRequest = AiMessageSaveRequest.builder()
                    .aiGameRoomId(roomId)
                    .gameId(request.getGameId())
                    .content(request.getContent())
                    .turnNumber(request.getTurnNumber())
                    .build();
            AiGameMessageDto savedMessage = aiGameMessageService.saveAiMessage(saveRequest);

            // AI 응답 완료 후 락 해제 및 다음 턴으로 진행 (WebSocket은 saveAiMessage에서 처리됨)
            int nextTurn = completeAiResponseAndProgressToNextTurn(roomId);

            log.info("AI 응답 처리 완료: roomId={}, nextTurn={}", roomId, nextTurn);

            AiGameMessageResponse response = AiGameMessageResponse.fromDto(savedMessage);
            return RsData.of("200", "AI 응답 생성 및 처리 완료", response);

        } catch (Exception e) {
            return handleAiResponseError(roomId, e, "AI 응답 처리 중 오류가 발생했습니다");
        }
    }

    /**
     * AI 오류를 처리합니다 (기존 reportAiError 로직)
     */
    public RsData<Void> handleAiError(String roomId, AiErrorRequest request) {
        log.error("AI 응답 생성 실패: roomId={}, error={}", roomId, request.getErrorMessage());

        try {
            // 에러 시스템 메시지 생성
            AiGameMessageDto errorMessage = aiGameMessageService.handleSystemMessage(
                    createErrorSystemMessage(roomId, request)
            );

            // 에러 메시지는 handleSystemMessage에서 자동으로 WebSocket 브로드캐스트됨

            // 락 해제 및 게임 일시정지
            aiGameStateService.unlockAfterAiResponse(roomId);
            aiGameStateService.pauseGame(roomId, "AI 응답 생성 오류: " + request.getErrorMessage());

            return RsData.of("200", "AI 오류 처리 완료", null);

        } catch (Exception e) {
            log.error("AI 에러 처리 중 오류 발생: roomId={}, error={}", roomId, e.getMessage(), e);
            return RsData.of("500", "AI 에러 처리 중 오류가 발생했습니다", null);
        }
    }

    /**
     * AI 응답 완료 후 락 해제 및 다음 턴으로 진행하는 공통 메서드
     */
    private int completeAiResponseAndProgressToNextTurn(String roomId) {
        aiGameStateService.unlockAfterAiResponse(roomId);
        return aiGameStateService.nextTurn(roomId);
    }

    /**
     * AI 응답 에러 처리 공통 메서드
     */
    private RsData<AiGameMessageResponse> handleAiResponseError(String roomId, Exception e, String errorMessage) {
        log.error("AI 응답 오류 발생: roomId={}, error={}", roomId, e.getMessage(), e);
        aiGameStateService.unlockAfterAiResponse(roomId);
        return RsData.of("500", errorMessage, null);
    }

    private AiGameMessageSendRequest createErrorSystemMessage(String roomId, AiErrorRequest request) {
        return AiGameMessageSendRequest.builder()
                .aiGameRoomId(roomId)
                .gameId(request.getGameId())
                .senderId(SYSTEM_SENDER_ID)
                .senderNickname(SYSTEM_SENDER_NICKNAME)
                .content("AI 응답 생성 중 오류가 발생했습니다: " + request.getErrorMessage())
                .messageType(org.com.dungeontalk.domain.aichat.common.AiMessageType.SYSTEM)
                .turnNumber(request.getTurnNumber())
                .messageOrder(ERROR_MESSAGE_ORDER)
                .build();
    }

    /**
     * AI 턴 처리 이벤트 리스너
     */
    @EventListener
    @Async("matchingTaskExecutor")
    public void handleAiTurnProcessEvent(AiTurnProcessEvent event) {
        processAiTurn(event.getAiGameRoomId(), event.getAiRequest());
    }

    /**
     * 게임 시작 시간 계산 (첫 번째 메시지 시간 기반)
     */
    private Long calculateGameStartTime(String roomId) {
        try {
            // 첫 번째 메시지 시간을 게임 시작 시간으로 사용
            List<AiGameMessageDto> allMessages = aiGameMessageService
                    .getContextMessages(roomId, 100, Integer.MAX_VALUE); // 모든 메시지 조회

            if (!allMessages.isEmpty()) {
                // 첫 번째 메시지의 생성 시간을 Unix timestamp로 변환
                return allMessages.get(allMessages.size() - 1).getCreatedAt()
                        .atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
            }
        } catch (Exception e) {
            log.warn("게임 시작 시간 계산 실패: {}", e.getMessage());
        }

        // 첫 메시지가 없으면 현재 시간 사용
        return System.currentTimeMillis() / 1000;
    }

    /**
     * AI에 의해 게임이 종료되었을 때 후처리를 담당하는 메소드.
     * 특히 게임 결과에 따른 경험치 지급 로직을 처리.
     *
     * @param roomId      현재 게임방 ID
     * @param characterId 현재 턴을 진행한 캐릭터 ID
     * @param aiResult    게임 종료 여부 및 결과가 포함된 AI의 응답 객체
     */
    private void handleGameEnd(String roomId, String characterId, AiServiceResponse aiResult) {
        try {
            log.info("🎯 TRPG 게임 종료 처리 시작: roomId={}, characterId={}", roomId, characterId);

            // 1. roomId로 AiGameRoom 엔티티를 조회.
            AiGameRoom room = aiGameRoomService.getGameRoomEntity(roomId);
            String settings = room.getGameSettings();
            Long worldId = null;

            // 2. GameSettings(JSON)에서 worldId를 파싱.
            //    (가정: gameSettings에 {"worldId": 1} 와 같이 worldId가 저장되어 있음)
            try {
                JsonNode rootNode = objectMapper.readTree(settings);
                if (rootNode.has("worldId")) {
                    worldId = rootNode.get("worldId").asLong();
                }
            } catch (JsonProcessingException e) {
                log.error("JSON 파싱 실패: {}", settings, e);
            }

            // 3. worldId를 찾지 못하면 경험치 지급 로직을 중단.
            if (worldId == null) {
                log.error("worldId를 찾을 수 없어 경험치 지급을 중단합니다. gameSettings: {}", settings);
                return;
            }

            // 4. AI 응답의 게임 단계를 보고 클리어 여부(1 또는 0)를 결정.
            int isCleared = "SUCCESS".equalsIgnoreCase(aiResult.getGamePhase()) ? 1 : 0;

            // 5. 경험치 처리를 위해 GameCharacterService에 요청을 보냄.
            GameResultRequest request = new GameResultRequest(characterId, worldId, isCleared);
            gameCharacterService.processGameResult(request);

            log.info("✅ TRPG 게임 종료 및 경험치 처리 완료: characterId={}, worldId={}, isCleared={}", characterId, worldId, isCleared);
        } catch (Exception e) {
            log.error("❌ TRPG 게임 종료 처리 실패: roomId={}, characterId={}, error={}",
                    roomId, characterId, e.getMessage(), e);
        }
    }
}

