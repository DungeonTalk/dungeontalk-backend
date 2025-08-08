package org.com.dungeontalk.domain.aichat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.aichat.dto.AiGameMessageDto;
import org.com.dungeontalk.domain.aichat.dto.request.AiErrorRequest;
import org.com.dungeontalk.domain.aichat.dto.request.AiGenerateRequest;
import org.com.dungeontalk.domain.aichat.dto.request.AiMessageSaveRequest;
import org.com.dungeontalk.domain.aichat.dto.request.AiResponseRequest;
import org.com.dungeontalk.domain.aichat.dto.response.AiGameMessageResponse;
import org.com.dungeontalk.domain.aichat.dto.response.ProcessingStatusResponse;
import org.com.dungeontalk.domain.aichat.service.AiGameMessageService;
import org.com.dungeontalk.domain.aichat.service.AiGameStateService;
import org.com.dungeontalk.domain.aichat.service.AiResponseService;
import org.com.dungeontalk.global.rsData.RsData;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import static org.com.dungeontalk.domain.aichat.common.AiChatConstants.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/aichat/ai-service")
@RequiredArgsConstructor
public class AiResponseController {

    private final AiGameMessageService aiGameMessageService;
    private final AiGameStateService aiGameStateService;
    private final AiResponseService aiResponseService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 내부에서 AI 응답을 생성하고 처리하는 엔드포인트
     * 프론트엔드에서 직접 호출하여 AI 응답을 요청할 때 사용
     */
    @PostMapping("/rooms/{roomId}/generate")
    public RsData<AiGameMessageResponse> generateAndProcessAiResponse(
            @PathVariable String roomId,
            @RequestBody AiGenerateRequest request) {
        
        log.info("AI 응답 생성 및 처리 요청: roomId={}, user={}, turn={}", 
                 roomId, request.getCurrentUser(), request.getTurnNumber());

        // AI 응답 처리 중으로 락 설정
        boolean locked = aiGameStateService.lockForAiResponse(roomId);
        if (!locked) {
            log.warn("AI 응답 처리 중 락 설정 실패 (이미 처리중): roomId={}", roomId);
            return RsData.of("400-1", "AI 응답이 이미 처리 중입니다", null);
        }

        try {
            // 컨텍스트 메시지 조회
            List<AiGameMessageDto> contextMessages = aiGameMessageService
                    .getContextMessages(roomId, DEFAULT_CONTEXT_MESSAGE_COUNT, request.getTurnNumber());

            // Python AI 서비스에서 응답 생성
            AiResponseService.AiResponseResult aiResult = aiResponseService.generateAiResponse(
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
                    .responseTime(aiResult.getResponseTime())
                    .aiSources(aiResult.getSources() != null ? String.join(",", aiResult.getSources()) : null)
                    .build();
            AiGameMessageDto savedMessage = aiGameMessageService.saveAiMessage(saveRequest);

            // WebSocket 브로드캐스트 및 처리 완료
            sendWebSocketMessage(roomId, savedMessage);
            int nextTurn = completeAiResponseAndProgressToNextTurn(roomId);

            log.info("AI 응답 생성 및 처리 완료: roomId={}, nextTurn={}, responseTime={}ms", 
                     roomId, nextTurn, aiResult.getResponseTime());

            AiGameMessageResponse response = AiGameMessageResponse.fromDto(savedMessage);

            return RsData.of("200-1", "AI 응답 생성 및 처리 완료", response);

        } catch (Exception e) {
            return handleAiResponseError(roomId, e, "AI 응답 생성 중 오류가 발생했습니다");
        }
    }

    /**
     * Python AI 서비스에서 생성된 응답을 받는 엔드포인트
     * AI 응답을 저장하고 WebSocket으로 브로드캐스트한다.
     */
    @PostMapping("/rooms/{roomId}/response")
    public RsData<AiGameMessageResponse> receiveAiResponse(
            @PathVariable String roomId,
            @RequestBody AiResponseRequest request) {

        log.info("AI 응답 수신: roomId={}, turn={}, responseTime={}ms", 
                 roomId, request.getTurnNumber(), request.getResponseTime());

        try {
            // AI 메시지 저장
            AiMessageSaveRequest saveRequest = AiMessageSaveRequest.builder()
                    .aiGameRoomId(roomId)
                    .gameId(request.getGameId())
                    .content(request.getContent())
                    .turnNumber(request.getTurnNumber())
                    .responseTime(request.getResponseTime())
                    .aiSources(request.getAiSources())
                    .build();
            AiGameMessageDto savedMessage = aiGameMessageService.saveAiMessage(saveRequest);

            // WebSocket 브로드캐스트 및 처리 완료
            sendWebSocketMessage(roomId, savedMessage);
            int nextTurn = completeAiResponseAndProgressToNextTurn(roomId);

            log.info("AI 응답 처리 완료: roomId={}, nextTurn={}", roomId, nextTurn);

            AiGameMessageResponse response = AiGameMessageResponse.fromDto(savedMessage);
            return RsData.of("200-1", "AI 응답 생성 및 처리 완료", response);

        } catch (Exception e) {
            return handleAiResponseError(roomId, e, "AI 응답 처리 중 오류가 발생했습니다");
        }
    }

    /**
     * AI 응답 생성 실패 시 호출하는 엔드포인트
     */
    @PostMapping("/rooms/{roomId}/response/error")
    public RsData<Void> reportAiError(
            @PathVariable String roomId,
            @RequestBody AiErrorRequest request) {

        log.error("AI 응답 생성 실패: roomId={}, error={}", roomId, request.getErrorMessage());

        try {
            // 에러 시스템 메시지 생성
            AiGameMessageDto errorMessage = aiGameMessageService.handleSystemMessage(
                    createErrorSystemMessage(roomId, request)
            );

            // WebSocket으로 에러 메시지 브로드캐스트
            sendWebSocketMessage(roomId, errorMessage);

            // 락 해제 및 게임 일시정지
            aiGameStateService.unlockAfterAiResponse(roomId);
            aiGameStateService.pauseGame(roomId, "AI 응답 생성 오류: " + request.getErrorMessage());

            return RsData.of("200-1", "AI 오류 처리 완료", null);

        } catch (Exception e) {
            log.error("AI 에러 처리 중 오류 발생: roomId={}, error={}", roomId, e.getMessage(), e);
            return RsData.of("500-1", "AI 에러 처리 중 오류가 발생했습니다", null);
        }
    }

    /**
     * AI 응답 처리 상태 확인 엔드포인트
     */
    @GetMapping("/rooms/{roomId}/processing-status")
    public RsData<ProcessingStatusResponse> getProcessingStatus(@PathVariable String roomId) {
        
        boolean isProcessing = aiGameStateService.isAiProcessing(roomId);
        boolean isSessionValid = aiGameStateService.isSessionValid(roomId);

        ProcessingStatusResponse response = new ProcessingStatusResponse(roomId, isProcessing, isSessionValid);
        return RsData.of("200-1", "처리 상태 조회 완료", response);
    }

    
    /**
     * WebSocket 메시지 전송 공통 메서드
     */
    private void sendWebSocketMessage(String roomId, Object message) {
        String destination = WEBSOCKET_DESTINATION_PREFIX + roomId;
        messagingTemplate.convertAndSend(destination, message);
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
        return RsData.of("500-1", errorMessage, null);
    }

    private org.com.dungeontalk.domain.aichat.dto.request.AiGameMessageSendRequest createErrorSystemMessage(
            String roomId, AiErrorRequest request) {
        
        org.com.dungeontalk.domain.aichat.dto.request.AiGameMessageSendRequest systemMessage = 
                new org.com.dungeontalk.domain.aichat.dto.request.AiGameMessageSendRequest();
        
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

}