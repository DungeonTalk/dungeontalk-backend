
## application-properties 설정

```properties
# 공통
spring.application.name=dungeontalk-application-test
spring.config.import=

# ---------- RDB: H2 (JPA 전용) ----------
spring.datasource.url=jdbc:h2:mem:dungeontalk;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA DDL (테스트는 보통 create-drop)
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false

# ---------- Mongo: Embedded Mongo ----------
# 임베디드 몽고 사용 (포트 0 = 랜덤)
spring.data.mongodb.database=test-db

# Mongo 인덱스 자동 생성
spring.data.mongodb.auto-index-creation=true

# 임베디드 Mongo 버전 지정 (Flapdoodle 사용 시)
spring.mongodb.embedded.version=6.0.5

# ---------- Redis ----------
# Repository/Service/Controller 슬라이스 테스트에서는 Redis 컨테이너 안 띄움
# (WebMvcTest, DataJpaTest, DataMongoTest 는 기본적으로 해당 빈 로딩 안함)
# 통합 테스트가 필요하면 Testcontainers로 별도 셋업 추천

# ---------- JWT (테스트 더미 값) ----------
jwt.secret=test-secret
jwt.accessexpiration=3600000
jwt.refreshexpiration=604800000

# ---------- 기타 모듈 세팅(테스트 기본값) ----------
ai.service.url=http://localhost:8001
ai.service.timeout=60000
ai.service.connect.timeout=10000

aichat.session.timeout-seconds=3600
aichat.session.turn-lock-timeout-seconds=300
aichat.context.message-count=5
aichat.websocket.destination-prefix=/sub/aichat/room/
aichat.message-order.turn-start=0
aichat.message-order.turn-end=9999
aichat.message-order.error=9998

chat.room.default-max-capacity=3

# DevTools 비활성 (테스트에서는 불필요)
spring.devtools.restart.enabled=false
spring.devtools.livereload.enabled=false

# 로그 과다 방지
logging.level.org.springframework.test=INFO
logging.level.org.springframework.data.mongodb.core.MongoTemplate=WARN
logging.level.org.springframework.data.mongodb.repository=WARN

# JPA는 Mongo 슬라이스 테스트에 불필요하므로 안전하게 제외
spring.autoconfigure.exclude=\
org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,\
org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
```

## Repository 테스트

- RDB ⇒ @DataJpaTest + H2
- Mongo ⇒ @DataMongoTest + Flapdoodle Embedded Mongo
- Redis ⇒ 굳이 띄우지 않고, Repository 레벨에서는 미사용 / 필요시 Testcontainers

- @DataJpaTest + H2 (PostgreSQL 모드)
  - 멤버/계정 등 RDB 엔티티 리포지토리 검증

- @DataMongoTest + Flapdoodle Embedded Mongo 
  - ChatMessageRepository, ChatRoomRepository, ChatRoomMemberRepository 
  - 페이징/인덱스/컴파운드 인덱스 검색 동작 검증


## Service 테스트 
- 순수 단위 테스트(Mokito/BDD)로 의존성 mock (Redis/JWT/메시징 등 외부는 격리)
- @ExtendWith(MockitoExtension.class)
  - ChatRoomService, ChatMessageService 등: 리포지토리/Redis/Publisher/JWT는 mock
  - BDD 스타일(when/then)로 정원 체크, 멱등성, 이벤트 발행 경계 검증
- ChatMessageService 테스트 
  - ErrorCode에 CHAT_INVALID_PAYLOAD, CHAT_INVALID_MESSAGE_TYPE, CHAT_MEMBER_NOT_FOUND가 존재한다고 가정(앞서 리팩토링 반영 기준). 
  - 엔티티/DTO 빌더 유무와 무관하게, 엔티티는 Mockito mock으로 써서 의존 최소화.

## 기타 설명

- @Nested는 “중첩 테스트 클래스”를 뜻해. 여기서 이렇게 구성한 이유는:

1. 도메인별로 시나리오를 묶기 위해
   processMessage, handleTalkMessage, getMessagesByRoomId처럼 기능 단위로 블록을 나누면
   각기 다른 가정/검증을 깔끔하게 묶을 수 있어. 테스트 리포트에서도
   ChatMessageService ▶ processMessage ▶ TALK: ...처럼 트리 구조로 읽히는 게 장점.
2. 각 블록별 전용 훅 사용
   각 @Nested 안에 @BeforeEach/@AfterEach를 따로 둘 수 있어서,
   그 기능에만 필요한 목/픽스처를 설정하기 수월해짐.
3. 가독성 & 유지보수성
   실패가 나도 “어느 기능의 어떤 케이스가 깨졌는지”가 즉시 드러나고,
   새로운 케이스를 추가할 때도 해당 블록에만 추가하면 됨.
4. 테스트 격리
   서로 다른 기능의 전제조건이 섞이지 않도록 논리적인 경계를 유지.

참고로, @Nested 클래스는 non-static inner class 여야 하고,
스프링 컨텍스트를 띄우는 통합 테스트(@SpringBootTest 등)에서는
불필요한 컨텍스트 로딩이 늘 수 있어 적절히 쓰는 게 좋음.
지금처럼 순수 단위 테스트(+Mockito) 환경에선 이 방식이 특히 깔끔함.
