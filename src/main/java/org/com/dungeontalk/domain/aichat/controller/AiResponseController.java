package org.com.dungeontalk.domain.aichat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.aichat.dto.AiGameMessageDto;
import org.com.dungeontalk.domain.aichat.dto.response.AiGameMessageResponse;
import org.com.dungeontalk.domain.aichat.service.AiGameMessageService;
import org.com.dungeontalk.domain.aichat.service.AiGameStateService;
import org.com.dungeontalk.domain.aichat.service.AiResponseService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<AiGameMessageResponse> generateAndProcessAiResponse(
            @PathVariable String roomId,
            @RequestBody AiGenerateRequest request) {
        
        log.info("AI 응답 생성 및 처리 요청: roomId={}, user={}, turn={}", 
                 roomId, request.getCurrentUser(), request.getTurnNumber());

        // AI 응답 처리 중으로 락 설정
        boolean locked = aiGameStateService.lockForAiResponse(roomId);
        if (!locked) {
            log.warn("AI 응답 처리 중 락 설정 실패 (이미 처리중): roomId={}", roomId);
            return ResponseEntity.badRequest().build();
        }

        try {
            // 컨텍스트 메시지 조회
            List<AiGameMessageDto> contextMessages = aiGameMessageService
                    .getContextMessages(roomId, 5, request.getTurnNumber());

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
            AiGameMessageDto savedMessage = aiGameMessageService.saveAiMessage(
                    roomId,
                    request.getGameId(),
                    aiResult.getContent(),
                    request.getTurnNumber(),
                    aiResult.getResponseTime(),
                    aiResult.getSources() != null ? String.join(",", aiResult.getSources()) : null
            );

            // WebSocket으로 실시간 브로드캐스트
            String destination = "/sub/aichat/room/" + roomId;
            messagingTemplate.convertAndSend(destination, savedMessage);

            // AI 응답 완료 후 락 해제 및 다음 턴으로 진행
            aiGameStateService.unlockAfterAiResponse(roomId);
            int nextTurn = aiGameStateService.nextTurn(roomId);

            log.info("AI 응답 생성 및 처리 완료: roomId={}, nextTurn={}, responseTime={}ms", 
                     roomId, nextTurn, aiResult.getResponseTime());

            AiGameMessageResponse response = AiGameMessageResponse.builder()
                    .messageId(savedMessage.getMessageId())
                    .aiGameRoomId(savedMessage.getAiGameRoomId())
                    .senderId(savedMessage.getSenderId())
                    .senderNickname(savedMessage.getSenderNickname())
                    .content(savedMessage.getContent())
                    .messageType(savedMessage.getMessageType())
                    .turnNumber(savedMessage.getTurnNumber())
                    .messageOrder(savedMessage.getMessageOrder())
                    .aiResponseTime(savedMessage.getAiResponseTime())
                    .createdAt(savedMessage.getCreatedAt())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("AI 응답 생성 및 처리 중 오류 발생: roomId={}, error={}", roomId, e.getMessage(), e);
            // 오류 발생 시 락 해제
            aiGameStateService.unlockAfterAiResponse(roomId);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Python AI 서비스에서 생성된 응답을 받는 엔드포인트
     * AI 응답을 저장하고 WebSocket으로 브로드캐스트한다.
     */
    @PostMapping("/rooms/{roomId}/response")
    public ResponseEntity<AiGameMessageResponse> receiveAiResponse(
            @PathVariable String roomId,
            @RequestBody AiResponseRequest request) {

        log.info("AI 응답 수신: roomId={}, turn={}, responseTime={}ms", 
                 roomId, request.getTurnNumber(), request.getResponseTime());

        try {
            // AI 메시지 저장
            AiGameMessageDto savedMessage = aiGameMessageService.saveAiMessage(
                    roomId,
                    request.getGameId(),
                    request.getContent(),
                    request.getTurnNumber(),
                    request.getResponseTime(),
                    request.getAiSources()
            );

            // WebSocket으로 실시간 브로드캐스트
            String destination = "/sub/aichat/room/" + roomId;
            messagingTemplate.convertAndSend(destination, savedMessage);

            // AI 응답 완료 후 락 해제 (다음 턴으로 진행)
            aiGameStateService.unlockAfterAiResponse(roomId);
            
            // 다음 턴으로 진행
            int nextTurn = aiGameStateService.nextTurn(roomId);

            log.info("AI 응답 처리 완료: roomId={}, nextTurn={}", roomId, nextTurn);

            AiGameMessageResponse response = AiGameMessageResponse.builder()
                    .messageId(savedMessage.getMessageId())
                    .aiGameRoomId(savedMessage.getAiGameRoomId())
                    .senderId(savedMessage.getSenderId())
                    .senderNickname(savedMessage.getSenderNickname())
                    .content(savedMessage.getContent())
                    .messageType(savedMessage.getMessageType())
                    .turnNumber(savedMessage.getTurnNumber())
                    .messageOrder(savedMessage.getMessageOrder())
                    .aiResponseTime(savedMessage.getAiResponseTime())
                    .createdAt(savedMessage.getCreatedAt())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("AI 응답 처리 중 오류 발생: roomId={}, error={}", roomId, e.getMessage(), e);
            // 오류 발생 시 락 해제
            aiGameStateService.unlockAfterAiResponse(roomId);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * AI 응답 생성 실패 시 호출하는 엔드포인트
     */
    @PostMapping("/rooms/{roomId}/response/error")
    public ResponseEntity<Void> reportAiError(
            @PathVariable String roomId,
            @RequestBody AiErrorRequest request) {

        log.error("AI 응답 생성 실패: roomId={}, error={}", roomId, request.getErrorMessage());

        try {
            // 에러 시스템 메시지 생성
            AiGameMessageDto errorMessage = aiGameMessageService.handleSystemMessage(
                    createErrorSystemMessage(roomId, request)
            );

            // WebSocket으로 에러 메시지 브로드캐스트
            String destination = "/sub/aichat/room/" + roomId;
            messagingTemplate.convertAndSend(destination, errorMessage);

            // 락 해제 및 게임 일시정지
            aiGameStateService.unlockAfterAiResponse(roomId);
            aiGameStateService.pauseGame(roomId, "AI 응답 생성 오류: " + request.getErrorMessage());

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("AI 에러 처리 중 오류 발생: roomId={}, error={}", roomId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * AI 응답 처리 상태 확인 엔드포인트
     */
    @GetMapping("/rooms/{roomId}/processing-status")
    public ResponseEntity<ProcessingStatusResponse> getProcessingStatus(@PathVariable String roomId) {
        
        boolean isProcessing = aiGameStateService.isAiProcessing(roomId);
        boolean isSessionValid = aiGameStateService.isSessionValid(roomId);

        ProcessingStatusResponse response = new ProcessingStatusResponse(roomId, isProcessing, isSessionValid);
        return ResponseEntity.ok(response);
    }

    private org.com.dungeontalk.domain.aichat.dto.request.AiGameMessageSendRequest createErrorSystemMessage(
            String roomId, AiErrorRequest request) {
        
        org.com.dungeontalk.domain.aichat.dto.request.AiGameMessageSendRequest systemMessage = 
                new org.com.dungeontalk.domain.aichat.dto.request.AiGameMessageSendRequest();
        
        systemMessage.setAiGameRoomId(roomId);
        systemMessage.setGameId(request.getGameId());
        systemMessage.setSenderId("SYSTEM");
        systemMessage.setSenderNickname("시스템");
        systemMessage.setContent("AI 응답 생성 중 오류가 발생했습니다: " + request.getErrorMessage());
        systemMessage.setMessageType(org.com.dungeontalk.domain.aichat.common.AiMessageType.SYSTEM);
        systemMessage.setTurnNumber(request.getTurnNumber());
        systemMessage.setMessageOrder(9998); // 에러 메시지는 마지막에서 두번째
        
        return systemMessage;
    }

    // Inner classes for request/response DTOs
    public static class AiGenerateRequest {
        private String gameId;
        private String currentUser;
        private String currentMessage;
        private int turnNumber;

        // Getters and Setters
        public String getGameId() { return gameId; }
        public void setGameId(String gameId) { this.gameId = gameId; }
        
        public String getCurrentUser() { return currentUser; }
        public void setCurrentUser(String currentUser) { this.currentUser = currentUser; }
        
        public String getCurrentMessage() { return currentMessage; }
        public void setCurrentMessage(String currentMessage) { this.currentMessage = currentMessage; }
        
        public int getTurnNumber() { return turnNumber; }
        public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }
    }

    public static class AiResponseRequest {
        private String gameId;
        private String content;
        private int turnNumber;
        private Long responseTime;
        private String aiSources;

        // Getters and Setters
        public String getGameId() { return gameId; }
        public void setGameId(String gameId) { this.gameId = gameId; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public int getTurnNumber() { return turnNumber; }
        public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }
        
        public Long getResponseTime() { return responseTime; }
        public void setResponseTime(Long responseTime) { this.responseTime = responseTime; }
        
        public String getAiSources() { return aiSources; }
        public void setAiSources(String aiSources) { this.aiSources = aiSources; }
    }

    public static class AiErrorRequest {
        private String gameId;
        private int turnNumber;
        private String errorMessage;
        private String errorCode;

        // Getters and Setters
        public String getGameId() { return gameId; }
        public void setGameId(String gameId) { this.gameId = gameId; }
        
        public int getTurnNumber() { return turnNumber; }
        public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    }

    public static class ProcessingStatusResponse {
        private final String roomId;
        private final boolean isProcessing;
        private final boolean isSessionValid;

        public ProcessingStatusResponse(String roomId, boolean isProcessing, boolean isSessionValid) {
            this.roomId = roomId;
            this.isProcessing = isProcessing;
            this.isSessionValid = isSessionValid;
        }

        public String getRoomId() { return roomId; }
        public boolean isProcessing() { return isProcessing; }
        public boolean isSessionValid() { return isSessionValid; }
    }
}