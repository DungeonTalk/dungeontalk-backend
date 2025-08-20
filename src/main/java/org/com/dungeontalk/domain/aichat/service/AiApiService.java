package org.com.dungeontalk.domain.aichat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.aichat.dto.AiGameMessageDto;
import org.com.dungeontalk.domain.aichat.dto.request.AiServiceRequest;
import org.com.dungeontalk.domain.aichat.dto.request.ContextMessage;
import org.com.dungeontalk.domain.aichat.dto.response.AiServiceResponse;
import org.com.dungeontalk.domain.aichat.dto.response.AiGameRoomResponse;
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
    private final AiGameRoomService aiGameRoomService;

    @Value("${ai.service.url:http://localhost:8001}")
    private String aiServiceUrl;

    @Value("${ai.service.timeout:60000}")
    private int aiServiceTimeout;

    /**
     * Python AI 서비스에서 응답 생성
     */
    public AiServiceResponse generateAiResponse(String gameId, String aiGameRoomId, 
                                             String currentUser, String currentMessage,
                                             List<AiGameMessageDto> contextMessages, int turnNumber,
                                             Long gameStartTime, Integer targetDuration, Object characterStats) {
        
        String url = aiServiceUrl + "/ai-response-enhanced";
        
        try {
            log.info("Python AI 서비스 호출 시작: roomId={}, user={}, turn={}", 
                     aiGameRoomId, currentUser, turnNumber);

            // 게임방 정보 조회하여 gameSettings 가져오기
            AiGameRoomResponse roomResponse = aiGameRoomService.getAiGameRoom(aiGameRoomId);
            String gameSettings = roomResponse.getGameSettings();
            
            // 게임 설정에서 세계관 추출
            String worldType = extractWorldTypeFromGameSettings(gameSettings);
            
            log.debug("게임 세계관 설정: {}, 추출된 세계관: {}", gameSettings, worldType);

            // 요청 데이터 구성 (시간 관리 및 캐릭터 스탯 추가)
            AiServiceRequest request = AiServiceRequest.builder()
                    .gameId(gameId)
                    .aiGameRoomId(aiGameRoomId)
                    .currentUser(currentUser)
                    .currentMessage(currentMessage)
                    .contextMessages(contextMessages.stream()
                            .map(this::convertToContextMessage)
                            .toList())
                    .turnNumber(turnNumber)
                    .gameSettings(gameSettings)
                    .worldType(worldType)
                    .gameStartTime(gameStartTime)
                    .targetDuration(targetDuration != null ? targetDuration : 15)
                    .characterStats(characterStats)
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
                        .worldType((String) responseBody.get("world_type"))
                        .docTypesUsed((List<String>) responseBody.get("doc_types_used"))
                        .gameTimeInfo((Map<String, Object>) responseBody.get("game_time_info"))
                        .build();

                log.info("Python AI 서비스 호출 성공: roomId={}, responseTime={}ms, sourcesCount={}, gamePhase={}, gameEnded={}", 
                         aiGameRoomId, result.getResponseTime(), 
                         result.getSources() != null ? result.getSources().size() : 0,
                         result.getGamePhase(), result.isGameEnded());

                // 게임 종료 로그 출력
                if (result.isGameEnded()) {
                    log.info("🎯 TRPG 게임 종료 감지: gameId={}, roomId={}", gameId, aiGameRoomId);
                }

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

    /**
     * 게임 설정에서 세계관 코드 추출
     * gameSettings 문자열을 분석하여 세계관을 추출합니다.
     */
    private String extractWorldTypeFromGameSettings(String gameSettings) {
        if (gameSettings == null || gameSettings.isEmpty()) {
            return "FANTASY"; // 기본값
        }
        
        String upperSettings = gameSettings.toUpperCase();
        
        // 키워드 기반 세계관 감지
        if (upperSettings.contains("판타지") || upperSettings.contains("마법") || 
            upperSettings.contains("FANTASY") || upperSettings.contains("중세")) {
            return "FANTASY";
        } else if (upperSettings.contains("SF") || upperSettings.contains("사이버") || 
                   upperSettings.contains("우주") || upperSettings.contains("미래")) {
            return "SF";
        } else if (upperSettings.contains("현대") || upperSettings.contains("MODERN") || 
                   upperSettings.contains("도시")) {
            return "MODERN";
        } else if (upperSettings.contains("사이버펑크") || upperSettings.contains("CYBERPUNK")) {
            return "CYBERPUNK";
        } else if (upperSettings.contains("스팀펑크") || upperSettings.contains("STEAMPUNK")) {
            return "STEAMPUNK";
        } else if (upperSettings.contains("공포") || upperSettings.contains("호러") || 
                   upperSettings.contains("HORROR")) {
            return "HORROR";
        } else if (upperSettings.contains("서부") || upperSettings.contains("WESTERN")) {
            return "WESTERN";
        } else if (upperSettings.contains("포스트") || upperSettings.contains("아포칼립스") || 
                   upperSettings.contains("POST_APOCALYPTIC")) {
            return "POST_APOCALYPTIC";
        }
        
        log.debug("세계관을 특정할 수 없어 기본값(FANTASY) 사용: {}", gameSettings);
        return "FANTASY"; // 기본값
    }

}