package org.com.dungeontalk.domain.aichat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.aichat.dto.AiGameMessageDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiResponseService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.service.url:http://localhost:8001}")
    private String aiServiceUrl;

    @Value("${ai.service.timeout:30000}")
    private int aiServiceTimeout;

    /**
     * Python AI 서비스에서 응답 생성
     */
    public AiResponseResult generateAiResponse(String gameId, String aiGameRoomId, 
                                             String currentUser, String currentMessage,
                                             List<AiGameMessageDto> contextMessages, int turnNumber) {
        
        String url = aiServiceUrl + "/ai-response";
        
        try {
            log.info("Python AI 서비스 호출 시작: roomId={}, user={}, turn={}", 
                     aiGameRoomId, currentUser, turnNumber);

            // 요청 데이터 구성
            AiResponseRequest request = AiResponseRequest.builder()
                    .gameId(gameId)
                    .aiGameRoomId(aiGameRoomId)
                    .currentUser(currentUser)
                    .currentMessage(currentMessage)
                    .contextMessages(contextMessages.stream()
                            .map(this::convertToContextMessage)
                            .toList())
                    .turnNumber(turnNumber)
                    .build();

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<AiResponseRequest> httpEntity = new HttpEntity<>(request, headers);

            // Python AI 서비스 호출
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    httpEntity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                AiResponseResult result = AiResponseResult.builder()
                        .content((String) responseBody.get("content"))
                        .responseTime(((Number) responseBody.get("response_time")).longValue())
                        .sources((List<String>) responseBody.get("sources"))
                        .build();

                log.info("Python AI 서비스 호출 성공: roomId={}, responseTime={}ms, sourcesCount={}", 
                         aiGameRoomId, result.getResponseTime(), 
                         result.getSources() != null ? result.getSources().size() : 0);

                return result;
            } else {
                throw new RuntimeException("AI 서비스 응답 오류: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Python AI 서비스 호출 실패: roomId={}, error={}", aiGameRoomId, e.getMessage(), e);
            throw new RuntimeException("AI 응답 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * AI 서비스 상태 확인
     */
    public boolean isAiServiceHealthy() {
        try {
            String healthUrl = aiServiceUrl + "/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(healthUrl, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String status = (String) response.getBody().get("status");
                boolean isHealthy = "healthy".equals(status);
                
                log.debug("AI 서비스 상태 확인: {}", isHealthy ? "정상" : "비정상");
                return isHealthy;
            }
            
            return false;
        } catch (Exception e) {
            log.warn("AI 서비스 상태 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    private ContextMessage convertToContextMessage(AiGameMessageDto messageDto) {
        return ContextMessage.builder()
                .messageType(messageDto.getMessageType().toString())
                .senderNickname(messageDto.getSenderNickname())
                .content(messageDto.getContent())
                .turnNumber(messageDto.getTurnNumber())
                .messageOrder(messageDto.getMessageOrder())
                .build();
    }

    // Inner classes for request/response DTOs
    @lombok.Builder
    @lombok.Data
    public static class AiResponseRequest {
        @com.fasterxml.jackson.annotation.JsonProperty("game_id")
        private String gameId;
        
        @com.fasterxml.jackson.annotation.JsonProperty("ai_game_room_id")
        private String aiGameRoomId;
        
        @com.fasterxml.jackson.annotation.JsonProperty("current_user")
        private String currentUser;
        
        @com.fasterxml.jackson.annotation.JsonProperty("current_message")
        private String currentMessage;
        
        @com.fasterxml.jackson.annotation.JsonProperty("context_messages")
        private List<ContextMessage> contextMessages;
        
        @com.fasterxml.jackson.annotation.JsonProperty("turn_number")
        private int turnNumber;
    }

    @lombok.Builder
    @lombok.Data
    public static class ContextMessage {
        private String messageType;
        private String senderNickname;
        private String content;
        private int turnNumber;
        private int messageOrder;
    }

    @lombok.Builder
    @lombok.Data
    public static class AiResponseResult {
        private String content;
        private Long responseTime;
        private List<String> sources;
    }
}