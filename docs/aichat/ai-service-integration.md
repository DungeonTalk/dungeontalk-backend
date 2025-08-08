# 🤖 AI Service Integration Guide

## 개요

DungeonTalk AI Chat 시스템은 외부 Python AI 서비스와 RESTful API 통신을 통해 AI 응답을 생성합니다.

## 🏗️ 통신 아키텍처

```
┌─────────────────┐    HTTP POST    ┌─────────────────┐
│   Spring Boot   │ ──────────────► │   Python AI     │
│   Backend       │                 │   Service       │
│                 │ ◄────────────── │   (Port 8001)   │
└─────────────────┘    JSON Response └─────────────────┘
```

## ⚙️ 설정

### application.yml 설정
```yaml
ai-service:
  base-url: "http://localhost:8001"
  endpoints:
    generate-response: "/ai-response"
  timeout: 30000  # 30초
  retry:
    max-attempts: 3
    delay: 1000   # 1초
```

### HTTP 클라이언트 설정
```java
@Configuration
public class HttpClientConfig {
    
    @Value("${ai-service.timeout:30000}")
    private int timeout;
    
    @Bean
    public RestTemplate aiServiceRestTemplate() {
        HttpComponentsClientHttpRequestFactory factory = 
            new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        
        return new RestTemplate(factory);
    }
}
```

## 📡 API 통신

### 1. 요청 데이터 구조

#### Java DTO (요청)
```java
@Data
public class AiResponseRequest {
    @JsonProperty("game_id")
    private String gameId;
    
    @JsonProperty("ai_game_room_id")
    private String aiGameRoomId;
    
    @JsonProperty("current_user")
    private String currentUser;
    
    @JsonProperty("turn_number")
    private Integer turnNumber;
    
    @JsonProperty("context_messages")
    private List<ContextMessage> contextMessages;
}

@Data
public class ContextMessage {
    @JsonProperty("message_type")
    private String messageType;  // "USER" or "AI"
    
    private String sender;
    private String content;
    
    @JsonProperty("turn_number")
    private Integer turnNumber;
    
    private LocalDateTime timestamp;
}
```

#### Python API 요청 예시
```json
{
  "game_id": "dungeon-adventure",
  "ai_game_room_id": "66b4a1234567890abcdef123",
  "current_user": "user123", 
  "turn_number": 3,
  "context_messages": [
    {
      "message_type": "USER",
      "sender": "user123",
      "content": "던전으로 들어갑니다",
      "turn_number": 1,
      "timestamp": "2025-08-08T01:00:00Z"
    },
    {
      "message_type": "AI",
      "sender": "AI",
      "content": "어두운 던전 입구에서 차가운 바람이 불어옵니다...",
      "turn_number": 1,
      "timestamp": "2025-08-08T01:00:30Z"
    },
    {
      "message_type": "USER",
      "sender": "user123",
      "content": "횃불을 들고 앞으로 나아갑니다",
      "turn_number": 2,
      "timestamp": "2025-08-08T01:01:00Z"
    }
  ]
}
```

### 2. 응답 데이터 구조

#### Python API 응답
```json
{
  "success": true,
  "ai_response": "횃불의 불빛이 던전 내부를 비춥니다. 벽에는 고대 문자들이 새겨져 있고, 멀리서 물방울 떨어지는 소리가 들립니다. 앞쪽에서 두 갈래 길이 보입니다 - 왼쪽은 좁은 통로, 오른쪽은 넓은 홀입니다.",
  "processing_time_ms": 1500,
  "model_info": {
    "model_name": "gpt-4",
    "version": "2024-08-06"
  },
  "metadata": {
    "response_length": 156,
    "context_tokens": 245,
    "response_tokens": 89
  }
}
```

#### Java DTO (응답)
```java
@Data
public class AiResponseResult {
    private boolean success;
    
    @JsonProperty("ai_response")
    private String aiResponse;
    
    @JsonProperty("processing_time_ms")
    private Long processingTimeMs;
    
    @JsonProperty("model_info")
    private ModelInfo modelInfo;
    
    private Map<String, Object> metadata;
}

@Data
public class ModelInfo {
    @JsonProperty("model_name")
    private String modelName;
    
    private String version;
}
```

## 🔧 서비스 구현

### AI 응답 서비스
```java
@Service
@Slf4j
public class AiResponseService {
    
    private final RestTemplate aiServiceRestTemplate;
    private final AiMessageService aiMessageService;
    private final MessagePublisher messagePublisher;
    
    @Value("${ai-service.base-url}")
    private String aiServiceBaseUrl;
    
    public AiResponseResult generateAiResponse(String roomId, String gameId) {
        try {
            // 1. 컨텍스트 메시지 수집
            List<ContextMessage> contextMessages = buildContextMessages(roomId);
            
            // 2. AI 요청 데이터 구성
            AiResponseRequest request = AiResponseRequest.builder()
                .gameId(gameId)
                .aiGameRoomId(roomId)
                .currentUser(SecurityUtils.getCurrentUsername())
                .turnNumber(getCurrentTurnNumber(roomId))
                .contextMessages(contextMessages)
                .build();
            
            // 3. Python AI 서비스 호출
            String endpoint = aiServiceBaseUrl + "/ai-response";
            
            log.info("AI 서비스 호출 시작 - Room: {}, Endpoint: {}", roomId, endpoint);
            long startTime = System.currentTimeMillis();
            
            ResponseEntity<AiResponseResult> response = aiServiceRestTemplate.postForEntity(
                endpoint, request, AiResponseResult.class);
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("AI 응답 수신 완료 - Room: {}, 처리시간: {}ms", roomId, processingTime);
            
            // 4. 응답 검증
            AiResponseResult result = response.getBody();
            if (result == null || !result.isSuccess()) {
                throw new AiServiceException("AI 서비스 응답 오류");
            }
            
            // 5. 메시지 저장 및 브로드캐스팅
            saveAndBroadcastAiMessage(roomId, result);
            
            return result;
            
        } catch (Exception e) {
            log.error("AI 응답 생성 실패 - Room: {}", roomId, e);
            throw new AiServiceException("AI 응답 생성 중 오류가 발생했습니다", e);
        }
    }
    
    private List<ContextMessage> buildContextMessages(String roomId) {
        // 최근 10개 메시지를 컨텍스트로 사용
        List<AiMessage> recentMessages = aiMessageService.getRecentMessages(roomId, 10);
        
        return recentMessages.stream()
            .map(this::convertToContextMessage)
            .collect(Collectors.toList());
    }
    
    private ContextMessage convertToContextMessage(AiMessage message) {
        return ContextMessage.builder()
            .messageType(message.getMessageType().name())
            .sender(message.getSender())
            .content(message.getContent())
            .turnNumber(message.getTurnNumber())
            .timestamp(message.getTimestamp())
            .build();
    }
}
```

## 🔄 재시도 및 오류 처리

### 재시도 메커니즘
```java
@Component
@Slf4j
public class AiServiceClient {
    
    @Retryable(
        value = {ResourceAccessException.class, HttpServerErrorException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public AiResponseResult callAiService(AiResponseRequest request) {
        try {
            ResponseEntity<AiResponseResult> response = restTemplate.postForEntity(
                aiServiceUrl, request, AiResponseResult.class);
            
            return response.getBody();
            
        } catch (HttpClientErrorException e) {
            log.error("AI 서비스 클라이언트 오류 - Status: {}, Response: {}", 
                e.getStatusCode(), e.getResponseBodyAsString());
            throw new AiServiceException("AI 서비스 클라이언트 오류", e);
            
        } catch (HttpServerErrorException e) {
            log.error("AI 서비스 서버 오류 - Status: {}", e.getStatusCode());
            throw e; // 재시도 가능
            
        } catch (ResourceAccessException e) {
            log.error("AI 서비스 연결 오류", e);
            throw e; // 재시도 가능
        }
    }
    
    @Recover
    public AiResponseResult recover(Exception e, AiResponseRequest request) {
        log.error("AI 서비스 호출 최종 실패 - Room: {}", request.getAiGameRoomId(), e);
        
        // 대체 응답 생성 또는 예외 발생
        return AiResponseResult.builder()
            .success(false)
            .aiResponse("죄송합니다. 현재 AI 서비스에 연결할 수 없습니다.")
            .build();
    }
}
```

### 타임아웃 처리
```java
@Configuration
public class HttpClientConfig {
    
    @Bean
    public RestTemplate aiServiceRestTemplate() {
        // 커넥션 풀 설정
        PoolingHttpClientConnectionManager connectionManager = 
            new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(20);
        connectionManager.setDefaultMaxPerRoute(10);
        
        // 타임아웃 설정
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(5000)        // 연결 타임아웃: 5초
            .setSocketTimeout(30000)        // 읽기 타임아웃: 30초
            .setConnectionRequestTimeout(3000) // 커넥션 풀 타임아웃: 3초
            .build();
        
        // HTTP 클라이언트 생성
        CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .build();
        
        HttpComponentsClientHttpRequestFactory factory = 
            new HttpComponentsClientHttpRequestFactory(httpClient);
        
        return new RestTemplate(factory);
    }
}
```

## 📊 모니터링 및 메트릭

### 성능 메트릭 수집
```java
@Service
@Slf4j
public class AiServiceMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter aiRequestCounter;
    private final Timer aiResponseTimer;
    private final Gauge aiServiceHealthGauge;
    
    public AiServiceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.aiRequestCounter = Counter.builder("ai.service.requests")
            .description("AI 서비스 요청 수")
            .register(meterRegistry);
        this.aiResponseTimer = Timer.builder("ai.service.response.time")
            .description("AI 서비스 응답 시간")
            .register(meterRegistry);
    }
    
    public <T> T recordAiServiceCall(Supplier<T> operation) {
        aiRequestCounter.increment();
        
        return Timer.Sample.start(meterRegistry)
            .stop(aiResponseTimer)
            .recordCallable(operation);
    }
    
    @EventListener
    public void handleAiServiceError(AiServiceErrorEvent event) {
        Counter.builder("ai.service.errors")
            .tag("error.type", event.getErrorType())
            .register(meterRegistry)
            .increment();
    }
}
```

### 헬스체크
```java
@Component
public class AiServiceHealthIndicator implements HealthIndicator {
    
    private final RestTemplate aiServiceRestTemplate;
    
    @Value("${ai-service.base-url}")
    private String aiServiceBaseUrl;
    
    @Override
    public Health health() {
        try {
            // AI 서비스 헬스체크 엔드포인트 호출
            ResponseEntity<String> response = aiServiceRestTemplate.getForEntity(
                aiServiceBaseUrl + "/health", String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return Health.up()
                    .withDetail("ai-service", "Available")
                    .withDetail("response-time", measureResponseTime())
                    .build();
            } else {
                return Health.down()
                    .withDetail("ai-service", "Unavailable")
                    .withDetail("status", response.getStatusCode())
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("ai-service", "Connection failed")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
    
    private long measureResponseTime() {
        long startTime = System.currentTimeMillis();
        try {
            aiServiceRestTemplate.getForEntity(aiServiceBaseUrl + "/health", String.class);
            return System.currentTimeMillis() - startTime;
        } catch (Exception e) {
            return -1;
        }
    }
}
```

## 🐍 Python AI 서비스 인터페이스

### FastAPI 서버 예시
```python
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Optional
import time
import logging

app = FastAPI(title="DungeonTalk AI Service")

class ContextMessage(BaseModel):
    message_type: str
    sender: str
    content: str
    turn_number: int
    timestamp: str

class AiResponseRequest(BaseModel):
    game_id: str
    ai_game_room_id: str
    current_user: str
    turn_number: int
    context_messages: List[ContextMessage]

class ModelInfo(BaseModel):
    model_name: str
    version: str

class AiResponseResult(BaseModel):
    success: bool
    ai_response: str
    processing_time_ms: int
    model_info: ModelInfo
    metadata: dict

@app.post("/ai-response", response_model=AiResponseResult)
async def generate_ai_response(request: AiResponseRequest):
    start_time = time.time()
    
    try:
        # AI 모델 호출 (예: OpenAI GPT)
        ai_response = await call_ai_model(request)
        
        processing_time = int((time.time() - start_time) * 1000)
        
        return AiResponseResult(
            success=True,
            ai_response=ai_response,
            processing_time_ms=processing_time,
            model_info=ModelInfo(
                model_name="gpt-4",
                version="2024-08-06"
            ),
            metadata={
                "context_length": len(request.context_messages),
                "response_length": len(ai_response)
            }
        )
        
    except Exception as e:
        logging.error(f"AI 응답 생성 실패: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/health")
async def health_check():
    return {"status": "healthy", "timestamp": time.time()}
```

## 🔧 트러블슈팅

### 일반적인 문제 해결

#### 1. 연결 타임아웃
```java
// 타임아웃 설정 조정
@Value("${ai-service.timeout:60000}")  // 60초로 증가
private int timeout;
```

#### 2. 메모리 부족
```java
// 컨텍스트 메시지 제한
private static final int MAX_CONTEXT_MESSAGES = 20;

private List<ContextMessage> buildContextMessages(String roomId) {
    return aiMessageService.getRecentMessages(roomId, MAX_CONTEXT_MESSAGES)
        .stream()
        .map(this::convertToContextMessage)
        .collect(Collectors.toList());
}
```

#### 3. 응답 지연
```java
// 비동기 처리
@Async
public CompletableFuture<AiResponseResult> generateAiResponseAsync(String roomId) {
    return CompletableFuture.completedFuture(generateAiResponse(roomId));
}
```