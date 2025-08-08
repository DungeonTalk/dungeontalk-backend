# 🔧 AI Chat System Configuration Guide

## 환경 설정

### application.yml 설정

```yaml
# AI Chat 시스템 설정
aichat:
  # 게임 설정
  game:
    max-participants: 6
    default-timeout: 300000  # 5분
    turn-timeout: 60000      # 1분
    
  # AI 서비스 설정
  ai-service:
    base-url: "http://localhost:8001"
    endpoints:
      generate-response: "/ai-response"
      health-check: "/health"
    timeout:
      connect: 5000      # 5초
      read: 30000        # 30초
    retry:
      max-attempts: 3
      delay: 1000        # 1초
      multiplier: 2
    
  # 메시지 설정
  message:
    max-length: 1000
    context-limit: 20    # 컨텍스트로 사용할 최대 메시지 수
    
  # 동시성 제어
  concurrency:
    lock-timeout: 30000  # 30초
    lock-prefix: "aichat:lock:"

# MongoDB 설정
spring:
  data:
    mongodb:
      host: localhost
      port: 27017
      database: dungeondb
      username: admin
      password: 1234
      authentication-database: admin

# Redis 설정 (Valkey)
valkey:
  session:
    host: localhost
    port: 6379
    database: 0
  cache:
    host: localhost
    port: 6380
    database: 0
  lock:
    host: localhost
    port: 6380
    database: 1

# WebSocket 설정
websocket:
  stomp:
    endpoint: "/ws"
    message-size-limit: 65536    # 64KB
    send-buffer-size-limit: 524288  # 512KB
    send-timeout: 20000          # 20초
  cors:
    allowed-origins: 
      - "http://localhost:3000"
      - "http://localhost:8080"
    allowed-headers: "*"
    allowed-methods: "*"

# JWT 설정
jwt:
  secret: your-secret-key-here
  expiration: 86400000  # 24시간
  
# 로깅 설정
logging:
  level:
    org.com.dungeontalk.domain.aichat: DEBUG
    org.springframework.web.socket: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 환경별 설정

#### 개발 환경 (application-dev.yml)
```yaml
aichat:
  ai-service:
    base-url: "http://localhost:8001"
  game:
    max-participants: 2  # 개발 시 적은 인원

logging:
  level:
    root: INFO
    org.com.dungeontalk: DEBUG

# 개발용 CORS 설정
websocket:
  cors:
    allowed-origins: "*"  # 개발 시에만 모든 오리진 허용
```

#### 운영 환경 (application-prod.yml)
```yaml
aichat:
  ai-service:
    base-url: "http://ai-service:8001"
  game:
    max-participants: 6

logging:
  level:
    root: WARN
    org.com.dungeontalk.domain.aichat: INFO

# 운영용 보안 설정
websocket:
  cors:
    allowed-origins: 
      - "https://your-domain.com"
```

## 🐳 Docker 설정

### docker-compose.yml
```yaml
version: '3.8'

services:
  # 메인 백엔드 서비스
  backend:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - MONGODB_HOST=mongodb
      - VALKEY_SESSION_HOST=valkey-session
      - VALKEY_CACHE_HOST=valkey-cache
      - AI_SERVICE_URL=http://ai-service:8001
    depends_on:
      - mongodb
      - valkey-session
      - valkey-cache
      - ai-service
    networks:
      - dungeontalk-network

  # AI 서비스
  ai-service:
    image: dungeontalk/ai-service:latest
    ports:
      - "8001:8001"
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
    networks:
      - dungeontalk-network

  # MongoDB
  mongodb:
    image: mongo:7.0
    ports:
      - "27017:27017"
    environment:
      - MONGO_INITDB_ROOT_USERNAME=admin
      - MONGO_INITDB_ROOT_PASSWORD=1234
      - MONGO_INITDB_DATABASE=dungeondb
    volumes:
      - mongodb_data:/data/db
    networks:
      - dungeontalk-network

  # Valkey (Redis) - 세션 저장
  valkey-session:
    image: valkey/valkey:7.2
    ports:
      - "6379:6379"
    volumes:
      - valkey_session_data:/data
    networks:
      - dungeontalk-network

  # Valkey (Redis) - 캐시 및 락
  valkey-cache:
    image: valkey/valkey:7.2
    ports:
      - "6380:6379"
    volumes:
      - valkey_cache_data:/data
    networks:
      - dungeontalk-network

volumes:
  mongodb_data:
  valkey_session_data:
  valkey_cache_data:

networks:
  dungeontalk-network:
    driver: bridge
```

### Dockerfile
```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/dungeontalk-backend-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 🔐 보안 설정

### JWT 설정 클래스
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration}")
    private int jwtExpiration;
    
    @Bean
    public JwtTokenProvider jwtTokenProvider() {
        return new JwtTokenProvider(jwtSecret, jwtExpiration);
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/api/v1/aichat/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter(), 
                           UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

### WebSocket 보안 설정
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:3000")
                .withSockJS();
    }
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new JwtChannelInterceptor(jwtTokenProvider));
    }
}

@Component
public class JwtChannelInterceptor implements ChannelInterceptor {
    
    private final JwtTokenProvider jwtTokenProvider;
    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = 
            MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                if (jwtTokenProvider.validateToken(token)) {
                    String username = jwtTokenProvider.getUsernameFromToken(token);
                    accessor.setUser(new UsernamePasswordAuthenticationToken(
                        username, null, Collections.emptyList()));
                }
            }
        }
        
        return message;
    }
}
```

## 📊 모니터링 설정

### Actuator 설정
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    export:
      prometheus:
        enabled: true
  health:
    custom:
      enabled: true
```

### 커스텀 헬스 인디케이터
```java
@Component
public class AiChatSystemHealthIndicator implements HealthIndicator {
    
    private final AiServiceHealthIndicator aiServiceHealth;
    private final MongoTemplate mongoTemplate;
    private final ValkeyService valkeyService;
    
    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();
        
        try {
            // AI 서비스 상태 확인
            Health aiServiceHealth = this.aiServiceHealth.health();
            builder.withDetail("ai-service", aiServiceHealth.getStatus());
            
            // MongoDB 상태 확인
            mongoTemplate.getCollection("test").estimatedDocumentCount();
            builder.withDetail("mongodb", "UP");
            
            // Redis 상태 확인
            valkeyService.ping();
            builder.withDetail("redis", "UP");
            
            builder.up();
            
        } catch (Exception e) {
            builder.down().withDetail("error", e.getMessage());
        }
        
        return builder.build();
    }
}
```

## 📈 성능 튜닝

### JVM 옵션
```bash
java -Xms512m -Xmx2g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/app/dumps/ \
     -jar app.jar
```

### 커넥션 풀 설정
```java
@Configuration
public class DatabaseConfig {
    
    @Bean
    public MongoClientSettings mongoClientSettings() {
        ConnectionPoolSettings poolSettings = ConnectionPoolSettings.builder()
            .maxSize(100)                    // 최대 연결 수
            .minSize(10)                     // 최소 연결 수
            .maxWaitTime(30, TimeUnit.SECONDS) // 최대 대기 시간
            .build();
        
        return MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(mongoUri))
            .applyToConnectionPoolSettings(builder -> 
                builder.applySettings(poolSettings))
            .build();
    }
}
```

### Redis 커넥션 풀 설정
```java
@Configuration
public class RedisConfig {
    
    @Bean
    public JedisPoolConfig jedisPoolConfig() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);        // 최대 연결 수
        poolConfig.setMaxIdle(32);          // 최대 유휴 연결 수
        poolConfig.setMinIdle(8);           // 최소 유휴 연결 수
        poolConfig.setTestOnBorrow(true);   // 연결 시 검증
        poolConfig.setTestOnReturn(true);   // 반환 시 검증
        poolConfig.setTestWhileIdle(true);  // 유휴 시 검증
        return poolConfig;
    }
}
```

## 🚨 알림 설정

### Slack 알림
```java
@Component
public class AlertService {
    
    @Value("${alert.slack.webhook-url}")
    private String slackWebhookUrl;
    
    @EventListener
    public void handleAiServiceError(AiServiceErrorEvent event) {
        SlackMessage message = SlackMessage.builder()
            .text("🚨 AI 서비스 오류 발생")
            .channel("#alerts")
            .attachment(SlackAttachment.builder()
                .color("danger")
                .title("AI Chat Service Error")
                .field("Room ID", event.getRoomId(), true)
                .field("Error Type", event.getErrorType(), true)
                .field("Timestamp", event.getTimestamp().toString(), true)
                .build())
            .build();
        
        sendSlackMessage(message);
    }
    
    private void sendSlackMessage(SlackMessage message) {
        // Slack Webhook 호출 구현
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForEntity(slackWebhookUrl, message, String.class);
    }
}
```

## 🔧 개발 도구 설정

### IDE 설정 (IntelliJ IDEA)
```properties
# .idea/workspace.xml
# Spring Boot 개발 도구 활성화
spring.devtools.restart.enabled=true
spring.devtools.livereload.enabled=true

# 로깅 레벨 설정
logging.level.org.com.dungeontalk.domain.aichat=DEBUG
```

### 테스트 설정
```yaml
# application-test.yml
spring:
  data:
    mongodb:
      host: localhost
      port: 27017
      database: dungeondb_test
  
aichat:
  ai-service:
    base-url: "http://localhost:8002"  # 테스트용 Mock 서비스
```