# 🧪 AI Chat System Testing Guide

## 테스트 개요

AI Chat 시스템의 모든 컴포넌트를 체계적으로 테스트하기 위한 가이드입니다.

## 🔧 테스트 도구

### 1. 내장 테스트 페이지

시스템에는 다음과 같은 테스트 도구가 내장되어 있습니다:

#### `/ai-chat-test.html` - 완전한 테스트 도구
- 전체 AI 채팅 플로우 테스트
- JWT 토큰 생성 및 관리
- WebSocket 연결 테스트
- 게임방 CRUD 테스트
- 실시간 메시지 테스트

#### `/ai-chat-simple-test.html` - 간단한 테스트 도구
- 기본 채팅 기능 테스트
- 빠른 프로토타입 검증

#### `/login-test.html` - 인증 테스트 도구
- JWT 토큰 생성
- 로그인/로그아웃 테스트
- 토큰 유효성 검증

### 2. 테스트 페이지 사용법

#### Step 1: JWT 토큰 생성
```javascript
// /login-test.html에서 토큰 생성
function generateTestToken() {
    fetch('/api/v1/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            username: 'testuser',
            password: 'password'
        })
    })
    .then(response => response.json())
    .then(data => {
        localStorage.setItem('jwt_token', data.accessToken);
        console.log('JWT Token:', data.accessToken);
    });
}
```

#### Step 2: WebSocket 연결 테스트
```javascript
// /ai-chat-test.html에서 WebSocket 연결
function connectWebSocket() {
    const token = localStorage.getItem('jwt_token');
    const socket = new SockJS('/ws');
    const stompClient = Stomp.over(socket);
    
    stompClient.connect(
        { Authorization: 'Bearer ' + token },
        function(frame) {
            console.log('Connected:', frame);
            subscribeToRoom('test-room-id');
        },
        function(error) {
            console.error('Connection failed:', error);
        }
    );
}
```

#### Step 3: 게임방 생성 및 테스트
```javascript
// 게임방 생성
function createTestRoom() {
    const token = localStorage.getItem('jwt_token');
    
    fetch('/api/v1/aichat/rooms', {
        method: 'POST',
        headers: {
            'Authorization': 'Bearer ' + token,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            gameId: 'test-game',
            title: '테스트 게임방',
            description: '테스트용 게임방입니다',
            maxParticipants: 4
        })
    })
    .then(response => response.json())
    .then(data => {
        console.log('Room created:', data);
        return data.data.id;
    });
}
```

## 🎯 단위 테스트

### 1. 서비스 레이어 테스트

#### AiGameRoomService 테스트
```java
@ExtendWith(MockitoExtension.class)
class AiGameRoomServiceTest {
    
    @Mock
    private AiGameRoomRepository gameRoomRepository;
    
    @Mock
    private ValkeyService valkeyService;
    
    @InjectMocks
    private AiGameRoomService gameRoomService;
    
    @Test
    @DisplayName("게임방 생성 테스트")
    void createGameRoom_Success() {
        // Given
        AiGameRoomCreateRequest request = AiGameRoomCreateRequest.builder()
            .gameId("test-game")
            .title("테스트 게임방")
            .maxParticipants(4)
            .build();
        
        AiGameRoom savedRoom = AiGameRoom.builder()
            .id("room-id")
            .gameId("test-game")
            .title("테스트 게임방")
            .status(GameStatus.CREATED)
            .build();
        
        when(gameRoomRepository.save(any(AiGameRoom.class)))
            .thenReturn(savedRoom);
        
        // When
        AiGameRoomResponse result = gameRoomService.createGameRoom(request, "user123");
        
        // Then
        assertThat(result.getId()).isEqualTo("room-id");
        assertThat(result.getStatus()).isEqualTo(GameStatus.CREATED);
        verify(gameRoomRepository).save(any(AiGameRoom.class));
    }
    
    @Test
    @DisplayName("게임방 시작 테스트")
    void startGame_Success() {
        // Given
        String roomId = "room-id";
        String creatorId = "user123";
        
        AiGameRoom gameRoom = AiGameRoom.builder()
            .id(roomId)
            .status(GameStatus.CREATED)
            .createdBy(creatorId)
            .build();
        
        when(gameRoomRepository.findById(roomId))
            .thenReturn(Optional.of(gameRoom));
        when(gameRoomRepository.save(any(AiGameRoom.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        gameRoomService.startGame(roomId, creatorId);
        
        // Then
        ArgumentCaptor<AiGameRoom> roomCaptor = ArgumentCaptor.forClass(AiGameRoom.class);
        verify(gameRoomRepository).save(roomCaptor.capture());
        
        AiGameRoom updatedRoom = roomCaptor.getValue();
        assertThat(updatedRoom.getStatus()).isEqualTo(GameStatus.ACTIVE);
        assertThat(updatedRoom.getTurnNumber()).isEqualTo(1);
    }
}
```

#### AI 응답 서비스 테스트
```java
@ExtendWith(MockitoExtension.class)
class AiResponseServiceTest {
    
    @Mock
    private RestTemplate aiServiceRestTemplate;
    
    @Mock
    private AiMessageService aiMessageService;
    
    @Mock
    private MessagePublisher messagePublisher;
    
    @InjectMocks
    private AiResponseService aiResponseService;
    
    @Test
    @DisplayName("AI 응답 생성 성공 테스트")
    void generateAiResponse_Success() {
        // Given
        String roomId = "room-id";
        String gameId = "test-game";
        
        List<ContextMessage> contextMessages = Arrays.asList(
            ContextMessage.builder()
                .messageType("USER")
                .content("던전으로 들어갑니다")
                .build()
        );
        
        AiResponseResult mockResult = AiResponseResult.builder()
            .success(true)
            .aiResponse("던전 입구에서 이상한 소리가 들립니다...")
            .processingTimeMs(1500L)
            .build();
        
        when(aiMessageService.getRecentMessages(roomId, 10))
            .thenReturn(Collections.emptyList());
        when(aiServiceRestTemplate.postForEntity(anyString(), any(), eq(AiResponseResult.class)))
            .thenReturn(ResponseEntity.ok(mockResult));
        
        // When
        AiResponseResult result = aiResponseService.generateAiResponse(roomId, gameId);
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAiResponse()).contains("던전 입구");
        verify(aiServiceRestTemplate).postForEntity(anyString(), any(), eq(AiResponseResult.class));
        verify(messagePublisher).publishAiMessage(any(), any());
    }
    
    @Test
    @DisplayName("AI 서비스 연결 실패 테스트")
    void generateAiResponse_ServiceFailure() {
        // Given
        String roomId = "room-id";
        String gameId = "test-game";
        
        when(aiMessageService.getRecentMessages(roomId, 10))
            .thenReturn(Collections.emptyList());
        when(aiServiceRestTemplate.postForEntity(anyString(), any(), eq(AiResponseResult.class)))
            .thenThrow(new ResourceAccessException("Connection timeout"));
        
        // When & Then
        assertThrows(AiServiceException.class, 
            () -> aiResponseService.generateAiResponse(roomId, gameId));
    }
}
```

### 2. 컨트롤러 테스트

#### REST API 컨트롤러 테스트
```java
@WebMvcTest(AiGameRoomController.class)
class AiGameRoomControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private AiGameRoomService gameRoomService;
    
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    
    @Test
    @DisplayName("게임방 생성 API 테스트")
    void createGameRoom_Success() throws Exception {
        // Given
        AiGameRoomCreateRequest request = AiGameRoomCreateRequest.builder()
            .gameId("test-game")
            .title("테스트 게임방")
            .build();
        
        AiGameRoomResponse response = AiGameRoomResponse.builder()
            .id("room-id")
            .gameId("test-game")
            .title("테스트 게임방")
            .status(GameStatus.CREATED)
            .build();
        
        when(gameRoomService.createGameRoom(any(), anyString()))
            .thenReturn(response);
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(anyString())).thenReturn("user123");
        
        // When & Then
        mockMvc.perform(post("/api/v1/aichat/rooms")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("S-1"))
                .andExpect(jsonPath("$.data.id").value("room-id"))
                .andExpect(jsonPath("$.data.status").value("CREATED"));
    }
}
```

#### WebSocket 컨트롤러 테스트
```java
@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
class AiChatStompControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @LocalServerPort
    private int port;
    
    private StompSession stompSession;
    private final BlockingQueue<String> blockingQueue = new LinkedBlockingQueue<>();
    
    @BeforeEach
    void setUp() throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJSClient(
            Arrays.asList(new WebSocketTransport(new StandardWebSocketClient()))));
        
        String url = String.format("ws://localhost:%d/ws", port);
        stompSession = stompClient.connect(url, new StompSessionHandlerAdapter()).get();
    }
    
    @Test
    @Order(1)
    @DisplayName("게임방 참여 메시지 테스트")
    void joinRoom_Success() throws Exception {
        // Given
        String roomId = "test-room";
        
        stompSession.subscribe("/sub/aichat/room/" + roomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }
            
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                blockingQueue.add((String) payload);
            }
        });
        
        // When
        stompSession.send("/pub/aichat/join", Map.of(
            "roomId", roomId,
            "username", "testuser"
        ));
        
        // Then
        String message = blockingQueue.poll(5, TimeUnit.SECONDS);
        assertThat(message).isNotNull();
        assertThat(message).contains("USER_JOINED");
    }
}
```

## 🌐 통합 테스트

### E2E 테스트 시나리오
```java
@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
class AiChatE2ETest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private AiGameRoomRepository gameRoomRepository;
    
    private String jwtToken;
    private String roomId;
    
    @Test
    @Order(1)
    @DisplayName("전체 플로우 테스트: 로그인 → 게임방 생성 → 게임 시작 → AI 채팅")
    void fullAiChatFlow() {
        // 1. 로그인
        jwtToken = login("testuser", "password");
        assertThat(jwtToken).isNotNull();
        
        // 2. 게임방 생성
        roomId = createGameRoom();
        assertThat(roomId).isNotNull();
        
        // 3. 게임 시작
        startGame(roomId);
        
        // 4. WebSocket 연결 및 채팅
        testChatting(roomId);
        
        // 5. AI 응답 생성
        generateAiResponse(roomId);
        
        // 6. 게임 종료
        endGame(roomId);
    }
    
    private String login(String username, String password) {
        LoginRequest request = new LoginRequest(username, password);
        
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
            "/api/v1/auth/login", request, LoginResponse.class);
        
        return response.getBody().getAccessToken();
    }
    
    private String createGameRoom() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        
        AiGameRoomCreateRequest request = AiGameRoomCreateRequest.builder()
            .gameId("e2e-test-game")
            .title("E2E 테스트 게임방")
            .build();
        
        HttpEntity<AiGameRoomCreateRequest> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<RsData<AiGameRoomResponse>> response = restTemplate.exchange(
            "/api/v1/aichat/rooms", HttpMethod.POST, entity, 
            new ParameterizedTypeReference<RsData<AiGameRoomResponse>>() {});
        
        return response.getBody().getData().getId();
    }
}
```

## 🚀 성능 테스트

### JMeter 테스트 계획

#### 1. WebSocket 연결 부하 테스트
```xml
<!-- JMeter WebSocket 테스트 계획 -->
<TestPlan>
  <hashTree>
    <ThreadGroup>
      <elementProp name="ThreadGroup.arguments" elementType="Arguments"/>
      <stringProp name="ThreadGroup.num_threads">100</stringProp>
      <stringProp name="ThreadGroup.ramp_time">10</stringProp>
      <stringProp name="ThreadGroup.duration">60</stringProp>
    </ThreadGroup>
    
    <WebSocketSampler>
      <stringProp name="WebSocketSampler.serverNameOrIp">localhost</stringProp>
      <stringProp name="WebSocketSampler.portNumber">8080</stringProp>
      <stringProp name="WebSocketSampler.path">/ws</stringProp>
      <stringProp name="WebSocketSampler.requestData">
        {"type":"CONNECT","headers":{"Authorization":"Bearer ${jwt_token}"}}
      </stringProp>
    </WebSocketSampler>
  </hashTree>
</TestPlan>
```

#### 2. AI 응답 생성 부하 테스트
```java
@Test
@DisplayName("AI 응답 생성 성능 테스트")
void aiResponsePerformanceTest() throws InterruptedException {
    int threadCount = 10;
    int requestsPerThread = 5;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount * requestsPerThread);
    
    List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
    
    for (int i = 0; i < threadCount; i++) {
        executor.submit(() -> {
            for (int j = 0; j < requestsPerThread; j++) {
                long startTime = System.currentTimeMillis();
                
                try {
                    aiResponseService.generateAiResponse("test-room", "test-game");
                    long responseTime = System.currentTimeMillis() - startTime;
                    responseTimes.add(responseTime);
                } catch (Exception e) {
                    System.err.println("Request failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            }
        });
    }
    
    latch.await(5, TimeUnit.MINUTES);
    executor.shutdown();
    
    // 결과 분석
    double avgResponseTime = responseTimes.stream()
        .mapToLong(Long::longValue)
        .average()
        .orElse(0.0);
    
    long maxResponseTime = responseTimes.stream()
        .mapToLong(Long::longValue)
        .max()
        .orElse(0L);
    
    System.out.println("평균 응답 시간: " + avgResponseTime + "ms");
    System.out.println("최대 응답 시간: " + maxResponseTime + "ms");
    System.out.println("총 요청 수: " + responseTimes.size());
    
    // 성능 기준 검증
    assertThat(avgResponseTime).isLessThan(5000.0); // 평균 5초 이내
    assertThat(maxResponseTime).isLessThan(30000L);  // 최대 30초 이내
}
```

## 🐛 테스트 데이터 관리

### 테스트 데이터 초기화
```java
@TestConfiguration
public class TestDataConfig {
    
    @Bean
    @Primary
    public MongoTemplate testMongoTemplate() {
        return new MongoTemplate(mongoClient(), "test_database");
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void initTestData() {
        // 테스트용 사용자 생성
        createTestUsers();
        
        // 테스트용 게임방 생성
        createTestRooms();
    }
    
    private void createTestUsers() {
        List<User> testUsers = Arrays.asList(
            User.builder().username("testuser1").password("password").build(),
            User.builder().username("testuser2").password("password").build()
        );
        
        userRepository.saveAll(testUsers);
    }
}
```

### Mock AI 서비스
```java
@TestConfiguration
public class MockAiServiceConfig {
    
    @Bean
    @Primary
    public RestTemplate mockAiServiceRestTemplate() {
        RestTemplate mockRestTemplate = Mockito.mock(RestTemplate.class);
        
        // Mock 응답 설정
        AiResponseResult mockResult = AiResponseResult.builder()
            .success(true)
            .aiResponse("이것은 테스트용 AI 응답입니다.")
            .processingTimeMs(500L)
            .build();
        
        when(mockRestTemplate.postForEntity(anyString(), any(), eq(AiResponseResult.class)))
            .thenReturn(ResponseEntity.ok(mockResult));
        
        return mockRestTemplate;
    }
}
```

## 📊 테스트 커버리지

### Jacoco 설정
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.8</version>
    <configuration>
        <excludes>
            <exclude>**/*Application.*</exclude>
            <exclude>**/*Config.*</exclude>
            <exclude>**/*DTO.*</exclude>
            <exclude>**/*Entity.*</exclude>
        </excludes>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### 테스트 실행 명령어
```bash
# 전체 테스트 실행
mvn test

# 특정 테스트 클래스 실행
mvn test -Dtest=AiGameRoomServiceTest

# 통합 테스트 실행
mvn test -Dtest=*IntegrationTest

# 커버리지 리포트 생성
mvn jacoco:report

# 성능 테스트 실행 (별도 프로파일)
mvn test -Pperformance-test
```