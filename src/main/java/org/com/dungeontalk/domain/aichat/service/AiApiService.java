package org.com.dungeontalk.domain.aichat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.aichat.dto.AiGameMessageDto;
import org.com.dungeontalk.domain.aichat.dto.request.AiServiceRequest;
import org.com.dungeontalk.domain.aichat.dto.request.ContextMessage;
import org.com.dungeontalk.domain.aichat.dto.response.AiServiceResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.com.dungeontalk.global.exception.ErrorCode;
import org.com.dungeontalk.global.exception.customException.AiChatException;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiApiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.service.url:http://localhost:8001}")
    private String aiServiceUrl;

    @Value("${ai.service.timeout:60000}")
    private int aiServiceTimeout;

    /**
     * Python AI 서비스에서 응답 생성
     */
    public AiServiceResponse generateAiResponse(String gameId, String aiGameRoomId, 
                                             String currentUser, String currentMessage,
                                             List<AiGameMessageDto> contextMessages, int turnNumber) {
        
        String url = aiServiceUrl + "/ai-response";
        
        try {
            log.info("Python AI 서비스 호출 시작: roomId={}, user={}, turn={}", 
                     aiGameRoomId, currentUser, turnNumber);

            // 요청 데이터 구성
            AiServiceRequest request = AiServiceRequest.builder()
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

            HttpEntity<AiServiceRequest> httpEntity = new HttpEntity<>(request, headers);

            // Python AI 서비스 호출
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    httpEntity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                // AI 응답 데이터 검증
                String content = (String) responseBody.get("content");
                if (content == null || content.trim().isEmpty()) {
                    throw new AiChatException(ErrorCode.AI_RESPONSE_PROCESSING_ERROR, "AI 응답 내용이 비어있습니다");
                }
                
                // 응답 시간 검증 및 기본값 설정
                Long responseTime = 0L;
                if (responseBody.get("response_time") instanceof Number) {
                    responseTime = ((Number) responseBody.get("response_time")).longValue();
                }
                
                AiServiceResponse result = AiServiceResponse.builder()
                        .content(content)
                        .responseTime(responseTime)
                        .sources((List<String>) responseBody.get("sources"))
                        .build();

                log.info("Python AI 서비스 호출 성공: roomId={}, responseTime={}ms, sourcesCount={}", 
                         aiGameRoomId, result.getResponseTime(), 
                         result.getSources() != null ? result.getSources().size() : 0);

                return result;
            } else {
                throw new RuntimeException("AI 서비스 응답 오류: " + response.getStatusCode());
            }

        } catch (ResourceAccessException e) {
            log.error("AI 서비스 연결 시간 초과 또는 네트워크 오류: roomId={}, error={}", aiGameRoomId, e.getMessage());
            throw new AiChatException(ErrorCode.AI_RESPONSE_TIMEOUT_ERROR, e);
        } catch (RestClientException e) {
            log.error("AI 서비스 호출 실패: roomId={}, error={}", aiGameRoomId, e.getMessage(), e);
            throw new AiChatException(ErrorCode.AI_RESPONSE_PROCESSING_ERROR, e);
        } catch (Exception e) {
            log.error("예상치 못한 AI 서비스 오류: roomId={}, error={}", aiGameRoomId, e.getMessage(), e);
            throw new AiChatException(ErrorCode.AI_RESPONSE_PROCESSING_ERROR, e);
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
        } catch (ResourceAccessException e) {
            log.warn("AI 서비스 연결 시간 초과: {}", e.getMessage());
            return false;
        } catch (RestClientException e) {
            log.warn("AI 서비스 상태 확인 실패: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("예상치 못한 AI 서비스 상태 확인 오류: {}", e.getMessage());
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

}