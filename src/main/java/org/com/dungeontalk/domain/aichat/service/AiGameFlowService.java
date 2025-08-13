package org.com.dungeontalk.domain.aichat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.aichat.dto.AiGameMessageDto;
import org.com.dungeontalk.domain.aichat.dto.request.AiErrorRequest;
import org.com.dungeontalk.domain.aichat.dto.request.AiGenerateRequest;
import org.com.dungeontalk.domain.aichat.dto.request.AiGameMessageSendRequest;
import org.com.dungeontalk.domain.aichat.dto.request.AiMessageSaveRequest;
import org.com.dungeontalk.domain.aichat.dto.request.AiResponseRequest;
import org.com.dungeontalk.domain.aichat.dto.response.AiGameMessageResponse;
import org.com.dungeontalk.global.rsData.RsData;
import org.springframework.stereotype.Service;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.com.dungeontalk.domain.aichat.event.AiTurnProcessEvent;
import static org.com.dungeontalk.domain.aichat.common.AiChatConstants.*;

import java.util.List;

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

            // Python AI 서비스에서 응답 생성
            AiApiService.AiResponseResult aiResult = aiApiService.generateAiResponse(
                    request.getGameId(),
                    roomId,
                    request.getCurrentUser(),
                    request.getCurrentMessage(),
                    contextMessages,
                    request.getTurnNumber()
            );

            // AI 메시지 저장
            AiMessageSaveRequest saveRequest = AiMessageSaveRequest.builder()
                    .aiGameRoomId(roomId)
                    .gameId(request.getGameId())
                    .content(aiResult.getContent())
                    .turnNumber(request.getTurnNumber())
                    .build();
            AiGameMessageDto savedMessage = aiGameMessageService.saveAiMessage(saveRequest);

            // AI 응답 완료 후 락 해제 및 다음 턴으로 진행 (WebSocket은 saveAiMessage에서 처리됨)
            int nextTurn = completeAiResponseAndProgressToNextTurn(roomId);

            log.info("AI 턴 처리 완료: roomId={}, nextTurn={}", roomId, nextTurn);

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
        AiGameMessageSendRequest systemMessage = new AiGameMessageSendRequest();
        
        systemMessage.setAiGameRoomId(roomId);
        systemMessage.setGameId(request.getGameId());
        systemMessage.setSenderId(SYSTEM_SENDER_ID);
        systemMessage.setSenderNickname(SYSTEM_SENDER_NICKNAME);
        systemMessage.setContent("AI 응답 생성 중 오류가 발생했습니다: " + request.getErrorMessage());
        systemMessage.setMessageType(org.com.dungeontalk.domain.aichat.common.AiMessageType.SYSTEM);
        systemMessage.setTurnNumber(request.getTurnNumber());
        systemMessage.setMessageOrder(ERROR_MESSAGE_ORDER);
        
        return systemMessage;
    }

    /**
     * AI 턴 처리 이벤트 리스너
     */
    @EventListener
    @Async("matchingTaskExecutor")
    public void handleAiTurnProcessEvent(AiTurnProcessEvent event) {
        processAiTurn(event.getAiGameRoomId(), event.getAiRequest());
    }
}