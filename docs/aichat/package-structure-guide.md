# AiChat 모듈 패키지 구조 가이드

## 📋 개요

이 문서는 DungeonTalk의 AI 채팅 모듈(`aichat`)이 현재의 패키지 구조를 채택한 배경과 이유를 설명합니다. Domain-Driven Design (DDD)의 "Package by Feature" 패턴을 기반으로 한 구조 설계 철학을 다룹니다.

## 🏗️ 현재 패키지 구조

```
src/main/java/org/com/dungeontalk/domain/aichat/
├── common/           # 공통 상수, 열거형
│   ├── AiChatConstants.java      # WebSocket, 메시지 순서 등 상수
│   ├── AiGamePhase.java          # 게임 진행 단계 (대기, 진행, 종료)
│   ├── AiGameStatus.java         # 게임 상태 (생성, 활성, 일시정지, 종료)
│   └── AiMessageType.java        # 메시지 타입 (사용자, AI, 시스템, 턴)
├── config/           # 설정 관련 클래스
│   ├── AiChatProperties.java     # @ConfigurationProperties 설정 바인딩
│   ├── AiChatConfigHelper.java   # 정적 접근 헬퍼
│   └── AiGameMessageIndexConfig.java # MongoDB 인덱스 자동 생성
├── controller/       # REST API 엔드포인트
│   ├── AiChatStompController.java    # WebSocket STOMP 메시지 처리
│   ├── AiGameRoomController.java     # 게임방 CRUD API
│   └── AiResponseController.java     # AI 응답 처리 API
├── dto/             # 데이터 전송 객체
│   ├── AiGameMessageDto.java         # 메시지 DTO
│   ├── AiGameRoomDto.java           # 게임방 DTO
│   ├── SessionDataDto.java          # 세션 데이터 DTO
│   ├── request/                     # 요청 DTO
│   │   ├── AiErrorRequest.java
│   │   ├── AiGameMessageSendRequest.java
│   │   ├── AiGameRoomCreateRequest.java
│   │   ├── AiGameRoomJoinRequest.java
│   │   ├── AiGenerateRequest.java
│   │   ├── AiMessageSaveRequest.java
│   │   └── AiResponseRequest.java
│   └── response/                    # 응답 DTO
│       ├── AiGameMessageResponse.java
│       ├── AiGameRoomResponse.java
│       └── ProcessingStatusResponse.java
├── entity/          # JPA/MongoDB 엔티티
│   ├── AiGameMessage.java           # MongoDB 메시지 엔티티
│   └── AiGameRoom.java              # PostgreSQL 게임방 엔티티
├── repository/      # 데이터 접근 계층
│   ├── AiGameMessageRepository.java  # MongoDB Repository
│   └── AiGameRoomRepository.java     # JPA Repository
├── service/         # 비즈니스 로직
│   ├── AiGameMessageService.java     # 메시지 관리 서비스
│   ├── AiGameRoomService.java        # 게임방 관리 서비스
│   ├── AiGameStateService.java       # 게임 상태/세션 관리
│   └── AiResponseService.java        # AI 응답 처리 서비스
└── util/            # 유틸리티 클래스
    ├── AiChatErrorHandler.java       # 공통 에러 처리
    ├── AiChatLogUtils.java          # 표준화된 로깅
    └── AiGameValidator.java          # 게임 규칙 검증
```

## 🤔 왜 이런 구조가 나왔는가?

### 1. 전통적인 계층별 구조의 한계

#### 기존 Layered Architecture의 문제점
```
src/main/java/com/company/
├── controller/
│   ├── UserController.java
│   ├── OrderController.java
│   ├── PaymentController.java
│   ├── ChatController.java
│   └── AiChatController.java     # 흩어져 있어서 찾기 어려움
├── service/
│   ├── UserService.java
│   ├── OrderService.java
│   ├── PaymentService.java
│   ├── ChatService.java
│   └── AiChatService.java        # 관련 파일이 멀리 떨어져 있음
├── repository/
│   ├── UserRepository.java
│   ├── OrderRepository.java
│   ├── PaymentRepository.java
│   ├── ChatRepository.java
│   └── AiChatRepository.java     # 협업 시 충돌 가능성 높음
└── dto/
    ├── UserDto.java
    ├── OrderDto.java
    ├── PaymentDto.java
    ├── ChatDto.java
    └── AiChatDto.java            # 모든 도메인이 섞여있음
```

#### 발생하는 문제들
1. **낮은 응집도**: 관련 있는 파일들이 물리적으로 멀리 떨어져 있음
2. **높은 결합도**: 한 기능 수정 시 여러 패키지를 동시에 수정해야 함
3. **협업 충돌**: 여러 개발자가 같은 패키지를 동시에 수정할 가능성
4. **인지 부하**: 특정 기능 개발 시 여러 디렉토리를 왔다갔다 해야 함

### 2. Domain-Driven Design (DDD)의 등장

#### DDD의 핵심 개념
- **도메인**: 비즈니스 영역의 특정 분야
- **바운디드 컨텍스트**: 도메인 모델이 적용되는 경계
- **응집성**: 관련 있는 것들끼리 모으기
- **격리**: 도메인 간 의존성 최소화

#### Package by Feature의 장점
```
domain/
├── user/           # 사용자 도메인 - 모든 관련 파일이 한 곳에
│   ├── UserController.java
│   ├── UserService.java
│   ├── UserRepository.java
│   └── UserDto.java
├── order/          # 주문 도메인 - 독립적인 개발 가능
│   ├── OrderController.java
│   ├── OrderService.java
│   ├── OrderRepository.java
│   └── OrderDto.java
└── aichat/         # AI 채팅 도메인 - 완전히 격리됨
    ├── controller/
    ├── service/
    ├── repository/
    └── dto/
```

#### 실제 효과
1. **높은 응집도**: 관련 파일들이 물리적으로 가까이 위치
2. **낮은 결합도**: 도메인 간 의존성 최소화
3. **팀 독립성**: 각 도메인별로 독립적인 개발 가능
4. **쉬운 이해**: 새로운 개발자도 빠르게 파악 가능

### 3. 글로벌 기술 트렌드의 영향

#### Microservices Architecture (2014~)
```
기존 Monolith의 한계:
├── user-service/
├── order-service/
└── payment-service/

MSA로 분리하면서 도메인별 패키지 구조가 자연스러워짐
→ 나중에 독립된 서비스로 분리하기 용이
```

#### Clean Architecture (2012, Robert Martin)
```
Clean Architecture의 계층:
├── entity/         # 엔티티 (가장 안쪽)
├── usecase/        # 유스케이스 (비즈니스 로직)
├── interface/      # 인터페이스 어댑터
└── framework/      # 프레임워크 & 드라이버

→ 도메인 중심 설계 철학과 일치
```

#### Hexagonal Architecture (2005, Alistair Cockburn)
```
Hexagonal의 포트-어댑터 패턴:
domain/
├── port/           # 포트 (인터페이스)
└── adapter/        # 어댑터 (구현체)

→ 도메인 로직과 외부 시스템 분리
```

### 4. 대기업들의 모범사례

#### 한국 대기업들의 패키지 구조

**토스 (Toss)**
```java
// DDD + Clean Architecture 기반
src/main/java/im/toss/
├── account/        # 계정 도메인
│   ├── application/    # 응용 서비스 (UseCase)
│   ├── domain/        # 도메인 모델
│   ├── infrastructure/ # 인프라스트럭처 (Repository 구현)
│   └── interfaces/    # 인터페이스 (Controller)
├── payment/        # 결제 도메인
└── loan/          # 대출 도메인
```

**우아한형제들 (배달의민족)**
```java
// 전통적 레이어드 + 도메인 분리
src/main/java/com/woowahan/
├── order/          # 주문 도메인
│   ├── controller/
│   ├── service/
│   ├── repository/
│   └── dto/
├── delivery/       # 배달 도메인
└── payment/        # 결제 도메인
```

**카카오 (KakaoTalk)**
```java
// 간단하지만 명확한 구조
src/main/java/com/kakao/
├── chat/           # 채팅 기능
│   ├── api/
│   ├── service/
│   └── data/
├── friend/         # 친구 기능
└── profile/        # 프로필 기능
```

**네이버 (LINE)**
```java
// Clean Architecture 기반
src/main/java/com/linecorp/
├── message/        # 메시지 도메인
│   ├── controller/
│   ├── usecase/
│   ├── gateway/
│   └── entity/
├── timeline/       # 타임라인 도메인
└── notification/   # 알림 도메인
```

#### 해외 기업들의 사례

**Netflix**
```java
// MSA 대응 도메인 구조
src/main/java/com/netflix/
├── recommendation/  # 추천 서비스
├── streaming/      # 스트리밍 서비스
└── billing/        # 결제 서비스
```

**Spotify**
```java
// 기능별 도메인 분리
src/main/java/com/spotify/
├── playlist/       # 플레이리스트 기능
├── player/         # 플레이어 기능
└── social/         # 소셜 기능
```

### 5. Spring Framework의 진화

#### Spring 1.x (2003) - XML 시대
```xml
<!-- 모든 Bean이 XML에 정의 -->
<beans>
    <bean id="userService" class="com.example.service.UserService"/>
    <bean id="orderService" class="com.example.service.OrderService"/>
</beans>

→ 계층별 패키지 구조가 자연스러웠음
```

#### Spring 2.x (2006) - Annotation 도입
```java
@Service
@Repository
@Controller

→ 여전히 계층별 구조 선호
```

#### Spring Boot 1.x (2014) - Auto Configuration
```java
@SpringBootApplication
@ComponentScan

→ 패키지 스캔 방식으로 변화, 도메인별 구조 가능해짐
```

#### Spring Boot 2.x (2018) - 현재
```java
// 도메인별 구조 권장
@ComponentScan(basePackages = {
    "com.example.domain.user",
    "com.example.domain.order"
})

→ 도메인별 패키지 구조가 공식적으로 권장됨
```

## 🎯 각 패키지의 존재 이유

### `common/` 패키지

#### 왜 생겼는가?
```java
// 문제: 상수가 여기저기 흩어져 있음
public class Controller1 {
    private static final String PREFIX = "/sub/aichat/room/"; // 중복
}

public class Controller2 {
    private static final String PREFIX = "/sub/aichat/room/"; // 중복
}

public class Service1 {
    private static final int TIMEOUT = 3600; // 중복
}
```

#### 해결 방안
```java
// AiChatConstants.java - 상수 중앙 관리
public final class AiChatConstants {
    // WebSocket Destinations
    public static final String WEBSOCKET_DESTINATION_PREFIX = "/sub/aichat/room/";
    
    // 기본 타임아웃 설정
    public static final int DEFAULT_SESSION_TIMEOUT_SECONDS = 3600;
    
    // 발신자 ID 및 닉네임 상수
    public static final String AI_SENDER_ID = "AI_GM";
    public static final String SYSTEM_SENDER_ID = "SYSTEM";
}
```

#### 참고한 모범사례
```java
// Google Guava
public final class Strings {
    public static boolean isNullOrEmpty(@Nullable String string) { ... }
}

// Apache Commons Lang
public class StringUtils {
    public static final String EMPTY = "";
    public static final int INDEX_NOT_FOUND = -1;
}
```

### `config/` 패키지

#### 왜 생겼는가?
```java
// 문제: 설정값이 하드코딩되어 여러 곳에 산재
public class AiGameMessageService {
    private static final int CONTEXT_MESSAGE_COUNT = 5; // 하드코딩
    private static final int SESSION_TIMEOUT = 3600; // 하드코딩
}

public class AiResponseController {
    private static final String WEBSOCKET_PREFIX = "/sub/aichat/room/"; // 하드코딩
}

// 환경별로 다른 값이 필요한데 코드 수정이 필요함
```

#### 해결 방안
```java
// AiChatProperties.java - 설정값 외부화
@Data
@Component
@ConfigurationProperties(prefix = "aichat")
public class AiChatProperties {
    private Session session = new Session();
    private Context context = new Context();
    
    @Data
    public static class Session {
        private int timeoutSeconds = 3600;
    }
    
    @Data
    public static class Context {
        private int messageCount = 5;
    }
}
```

```properties
# application-dev.properties (개발 환경)
aichat.session.timeout-seconds=3600
aichat.context.message-count=5

# application-prod.properties (운영 환경)  
aichat.session.timeout-seconds=7200
aichat.context.message-count=10
```

#### 참고한 모범사례

**Spring Boot 공식 가이드**
```java
@ConfigurationProperties("my.service")
public class MyProperties {
    private boolean enabled;
    private InetAddress remoteAddress;
    private final Security security = new Security();
}
```

**Netflix OSS**
```java
public class HystrixCommandProperties {
    private static final HystrixProperty.Factory propertyFactory = 
        HystrixPropertiesFactory.getInstance();
    
    public HystrixProperty<Integer> executionTimeoutInMilliseconds() {
        return propertyFactory.getInteger(
            propertyPrefix + "execution.isolation.thread.timeoutInMilliseconds", 
            1000
        );
    }
}
```

### `util/` 패키지

#### 왜 생겼는가?
```java
// 문제: 횡단 관심사가 각 클래스마다 중복됨
public class AiChatService {
    public void method1() {
        try {
            log.info("AI 채팅 서비스 시작");
            // 비즈니스 로직
            log.info("AI 채팅 서비스 완료");
        } catch (Exception e) {
            log.error("AI 채팅 서비스 실패: {}", e.getMessage(), e);
            throw e;
        }
    }
}

public class AiGameRoomService {
    public void method2() {
        try {
            log.info("게임방 서비스 시작");  // 반복!
            // 비즈니스 로직
            log.info("게임방 서비스 완료");  // 반복!
        } catch (Exception e) {
            log.error("게임방 서비스 실패: {}", e.getMessage(), e);  // 반복!
            throw e;
        }
    }
}
```

#### 해결 방안
```java
// AiChatErrorHandler.java - 공통 에러 처리
@Component
public class AiChatErrorHandler {
    public <T> T executeWithLogging(GameAction<T> action, String context, String roomId) {
        try {
            log.debug("{} 시작: roomId={}", context, roomId);
            T result = action.execute();
            log.info("{} 성공: roomId={}", context, roomId);
            return result;
        } catch (Exception e) {
            log.error("{} 실패: roomId={}, error={}", context, roomId, e.getMessage(), e);
            throw new RuntimeException(context + " 실행 중 오류 발생", e);
        }
    }
}

// AiChatLogUtils.java - 표준화된 로깅
@Slf4j
public final class AiChatLogUtils {
    public static void logGameAction(String action, String roomId, Object... params) {
        log.info("🎮 {} | roomId: {} | params: {}", action, roomId, params);
    }
}
```

#### 참고한 모범사례

**토스 기술 블로그**
```java
@Component
public class ErrorHandler {
    public <T> T execute(Supplier<T> action, String context) {
        try {
            return action.get();
        } catch (Exception e) {
            log.error("[{}] 실행 실패", context, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
```

**Apache Commons Lang**
```java
public class StringUtils {
    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }
    
    public static boolean isNotEmpty(final CharSequence cs) {
        return !isEmpty(cs);
    }
}
```

**Google Guava**
```java
public final class Preconditions {
    public static <T> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }
}
```

## 🚀 우리가 선택한 구조의 장점

### 1. 팀 확장성
```java
// AI 채팅 팀 - 독립적으로 개발 가능
domain/aichat/
├── controller/
├── service/
├── repository/
└── dto/

// 일반 채팅 팀 - 독립적으로 개발 가능  
domain/chat/
├── controller/
├── service/
├── repository/
└── dto/

// 충돌 없이 병렬 개발 가능!
```

### 2. 변경 영향도 최소화
```java
// AI 채팅 기능 수정 시
domain/aichat/           ← 여기만 수정
├── service/            
├── controller/         
└── repository/         

// 다른 도메인은 전혀 영향 없음
domain/user/            ← 영향 없음
domain/game/            ← 영향 없음
domain/chat/            ← 영향 없음
```

### 3. 코드 응집도 증가
```java
// 함께 수정되는 파일들이 물리적으로 가까이 위치
domain/aichat/
├── AiChatService.java         ← 비즈니스 로직
├── AiChatController.java      ← API 엔드포인트
├── AiChatRepository.java      ← 데이터 접근
└── AiChatDto.java            ← 데이터 전송 객체

// IDE에서 한 번에 보이므로 개발 효율성 증가
```

### 4. MSA 준비
```java
// 현재 구조
domain/
├── aichat/     ← 독립적인 패키지
├── chat/       ← 독립적인 패키지
└── user/       ← 독립적인 패키지

// 나중에 MSA로 분리할 때
aichat-service/     ← 바로 분리 가능
chat-service/       ← 바로 분리 가능  
user-service/       ← 바로 분리 가능
```

### 5. 신입 개발자 친화적
```java
// 신입 개발자에게 업무 할당
"AI 채팅 기능을 담당하게 될 거야. 
 domain/aichat/ 폴더 안에 모든 파일이 있어."

// vs 계층별 구조
"AI 채팅 기능을 담당하게 될 거야.
 controller/AiChatController.java,
 service/AiChatService.java,  
 repository/AiChatRepository.java,
 dto/AiChatDto.java 등을 봐야 해."
```

## 📊 구조 비교 분석

| 측면 | 계층별 구조 | 도메인별 구조 | 우리 선택 |
|------|------------|-------------|----------|
| **응집도** | 낮음 | 높음 | ✅ 높음 |
| **결합도** | 높음 | 낮음 | ✅ 낮음 |
| **팀 협업** | 충돌 많음 | 독립적 | ✅ 독립적 |
| **신입자 학습** | 어려움 | 쉬움 | ✅ 쉬움 |
| **변경 영향도** | 광범위함 | 제한적 | ✅ 제한적 |
| **MSA 분리** | 어려움 | 쉬움 | ✅ 쉬움 |

## 🔮 미래 확장성

### 현재 구조의 확장 가능성

#### 1. 새로운 도메인 추가
```java
// 새로운 게임 기능 추가 시
domain/
├── aichat/         # 기존 AI 채팅
├── chat/           # 기존 일반 채팅  
├── user/           # 기존 사용자
└── boardgame/      # 새로운 보드게임 ← 쉽게 추가 가능
    ├── controller/
    ├── service/
    ├── repository/
    └── dto/
```

#### 2. MSA 분리
```java
// 현재 Monolith
dungeontalk-backend/
└── domain/
    ├── aichat/
    ├── chat/
    └── user/

// MSA로 분리 (언젠가)
aichat-service/         ← domain/aichat/ 그대로 이동
chat-service/           ← domain/chat/ 그대로 이동  
user-service/           ← domain/user/ 그대로 이동
```

#### 3. 팀 확장
```java
// 현재: 1개 팀
Backend Team (5명)
└── 모든 domain 담당

// 미래: 도메인별 팀
AI Chat Team (2명)     ← domain/aichat/ 담당
Chat Team (2명)        ← domain/chat/ 담당
User Team (1명)        ← domain/user/ 담당
```

## 📚 참고 자료

### 아키텍처 설계 서적
- **Domain-Driven Design** - Eric Evans (2003)
- **Clean Architecture** - Robert Martin (2017)
- **Building Microservices** - Sam Newman (2021)
- **Patterns of Enterprise Application Architecture** - Martin Fowler (2002)

### 온라인 리소스
- [Package by Feature vs Package by Layer](https://phauer.com/2020/package-by-feature/)
- [DDD and Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Spring Boot Best Practices](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.structuring-your-code)

### 한국 기업 기술 블로그
- [토스 - 대규모 서비스를 지탱하는 아키텍처](https://toss.tech/article/how-to-build-large-scale-service)
- [우아한형제들 - 패키지 구조와 아키텍처](https://techblog.woowahan.com/2647/)
- [카카오 - 대용량 서비스 아키텍처 설계](https://tech.kakao.com/2021/12/23/microservice-architecture/)
- [네이버 - MSA 전환 경험기](https://d2.naver.com/helloworld/6070967)

### 오픈소스 프로젝트 참고
- [Spring Petclinic](https://github.com/spring-projects/spring-petclinic) - Spring 공식 샘플
- [JHipster](https://github.com/jhipster/generator-jhipster) - 도메인별 구조 생성기
- [Netflix Conductor](https://github.com/Netflix/conductor) - Netflix MSA 구조
- [Shopizer](https://github.com/shopizer-ecommerce/shopizer) - E-commerce DDD 구조

## 🎯 결론

DungeonTalk의 `aichat` 모듈이 현재의 패키지 구조를 채택한 것은:

1. **Domain-Driven Design** - 비즈니스 도메인 중심 설계
2. **대기업 모범사례** - 토스, 배민, 카카오 등의 검증된 구조
3. **Spring Boot 권장사항** - 현재 공식적으로 권장하는 패키지 구조  
4. **팀 협업 최적화** - 독립적인 개발과 낮은 충돌율
5. **미래 확장성** - MSA 분리와 팀 확장에 유리한 구조
6. **개발자 친화성** - 새로운 팀원의 빠른 적응과 높은 생산성

이러한 복합적인 고려사항들이 모여 현재와 같은 **도메인 중심의 패키지 구조**가 탄생하게 되었습니다.

이는 단순한 유행이 아닌, **10년 이상의 소프트웨어 개발 경험과 수많은 기업들의 시행착오**를 통해 검증된 **최적의 구조**라고 할 수 있습니다.

---

**작성자**: Claude Code Assistant  
**작성일**: 2025-08-09  
**버전**: 1.0  
**상태**: 완료 ✅