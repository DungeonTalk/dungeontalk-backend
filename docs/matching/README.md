# 랜덤 매칭 시스템 설계 문서

## 개요

던전톡의 랜덤 매칭 시스템은 세계관별로 3명의 플레이어를 자동 매칭하여 AI 게임방과 일반 채팅방을 동시에 생성하는 시스템입니다.

## 핵심 기능

### 1. 기본 매칭 플로우
1. 사용자가 세계관(판타지/SF/현대) 선택
2. 매칭 큐에 참가하여 대기
3. 3명이 모이면 자동으로 AiGameRoom + ChatRoom 생성
4. WebSocket으로 실시간 매칭 완료 알림

### 2. 실시간 업데이트
- 현재 대기 인원 실시간 표시 (N/3명)
- 대기 시간 카운터
- 세계관별 대기 인원 미리 보기

### 3. 사용자 편의 기능
- 언제든지 매칭 취소 가능 (패널티 없음)
- 브라우저 종료 시 자동 큐 정리
- 중복 매칭 요청 방지

## 시스템 아키텍처

### 도메인 구조
```
domain/matching/
├── common/
│   ├── WorldType.java          # 세계관 enum (FANTASY, SF, MODERN)
│   ├── MatchingStatus.java     # 매칭 상태 enum
│   └── MatchingConstants.java  # 상수 정의
├── controller/
│   └── MatchingController.java # REST API
├── service/
│   ├── MatchingService.java    # 핵심 매칭 로직
│   └── MatchingQueueManager.java # Redis 큐 관리
├── dto/
│   ├── request/
│   │   ├── MatchingJoinRequest.java
│   │   └── MatchingCancelRequest.java
│   └── response/
│       ├── MatchingStatusResponse.java
│       └── MatchingCompleteResponse.java
└── config/
    └── MatchingWebSocketConfig.java
```

### Redis 데이터 구조

#### 큐 관리
```
# 세계관별 대기 큐
matching:queue:FANTASY → List ["userId1", "userId2"]
matching:queue:SF      → List ["userId3"]  
matching:queue:MODERN  → List ["userId4", "userId5"]

# 사용자별 매칭 정보 (TTL: 1시간)
matching:user:{userId} → Hash {
  worldType: "FANTASY",
  status: "WAITING",
  joinedAt: "2024-01-15T10:30:00",
  queuePosition: 2
}
```

#### 매칭 완료 세션
```
# 매칭 완료된 세션 정보 (TTL: 7일)
matching:session:{sessionId} → Hash {
  participants: "userId1,userId2,userId3",
  worldType: "FANTASY", 
  aiGameRoomId: "ai-room-456",
  chatRoomId: "chat-room-789",
  createdAt: "2024-01-15T10:35:00"
}
```

#### 통계 데이터
```
# 세계관별 실시간 통계
matching:stats:{worldType} → Hash {
  currentWaiting: 5,
  dailyMatches: 45,
  averageWaitTime: 28
}
```

## API 설계

### REST Endpoints
```
POST   /api/matching/join           # 매칭 큐 참가
DELETE /api/matching/cancel         # 매칭 취소  
GET    /api/matching/status/{userId} # 개인 매칭 상태 조회
GET    /api/matching/queue/stats    # 전체 큐 현황 조회
```

### WebSocket Events
```
# 구독 채널
/sub/matching/user/{userId}         # 개인 매칭 상태 업데이트
/sub/matching/queue/{worldType}     # 세계관별 큐 상태

# 발행 채널  
/pub/matching/join                  # 매칭 참가
/pub/matching/cancel                # 매칭 취소
```

## 매칭 로직

### 1. 매칭 참가 프로세스
```java
public MatchingStatusResponse joinMatching(String userId, WorldType worldType) {
    // 1. 중복 참가 체크
    if (isAlreadyInQueue(userId)) {
        throw new MatchingException("이미 매칭 대기 중입니다");
    }
    
    // 2. 큐에 사용자 추가
    String queueKey = "matching:queue:" + worldType;
    redisTemplate.opsForList().leftPush(queueKey, userId);
    
    // 3. 사용자 상태 저장  
    saveUserMatchingInfo(userId, worldType);
    
    // 4. 매칭 가능 여부 확인
    Long queueSize = redisTemplate.opsForList().size(queueKey);
    if (queueSize >= 3) {
        processMatching(worldType);
    }
    
    return MatchingStatusResponse.of(userId, queueSize.intValue());
}
```

### 2. 매칭 완료 처리
```java
@Transactional
public void processMatching(WorldType worldType) {
    // 1. 큐에서 3명 추출 (atomic)
    List<String> participants = extractThreeUsers(worldType);
    
    // 2. AI 게임방 생성
    AiGameRoom aiGameRoom = aiGameRoomService.createAiGameRoom(
        AiGameRoomCreateRequest.builder()
            .gameId(generateGameId())
            .roomName(worldType.getDisplayName() + " 랜덤 매칭")
            .maxParticipants(3)
            .gameSettings(worldType.getGameSettings())
            .creatorId(participants.get(0))
            .build()
    );
    
    // 3. 일반 채팅방 생성
    ChatRoom chatRoom = chatRoomService.createRoom(
        ChatRoomCreateRequestDto.builder()
            .roomName(worldType.getDisplayName() + " 채팅")
            .roomType(ChatRoomType.GAME)
            .mode(ChatMode.PUBLIC)
            .participantIds(participants)
            .build()
    );
    
    // 4. 매칭 세션 정보 저장
    saveMatchingSession(aiGameRoom.getId(), chatRoom.getId(), participants);
    
    // 5. WebSocket으로 매칭 완료 알림
    notifyMatchingComplete(participants, aiGameRoom.getId(), chatRoom.getId());
}
```

## 예외 상황 처리

### 1. 브라우저 종료/네트워크 끊김
```java
@EventListener
public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
    String userId = extractUserIdFromSession(event.getSessionId());
    if (userId != null) {
        cancelMatching(userId);
    }
}
```

### 2. 서버 재시작 시 큐 정리
```java
@EventListener(ApplicationReadyEvent.class)
public void cleanupOrphanedQueues() {
    for (WorldType worldType : WorldType.values()) {
        String queueKey = "matching:queue:" + worldType;
        
        // TTL 만료된 사용자들 제거
        List<String> users = redisTemplate.opsForList().range(queueKey, 0, -1);
        users.forEach(userId -> {
            if (isUserSessionExpired(userId)) {
                redisTemplate.opsForList().remove(queueKey, 0, userId);
            }
        });
    }
}
```

### 3. 동시 매칭 완료 처리
```java
@Transactional
public synchronized boolean tryProcessMatching(WorldType worldType) {
    String lockKey = "matching:lock:" + worldType;
    
    return redisTemplate.execute(new SessionCallback<Boolean>() {
        @Override
        public Boolean execute(RedisOperations operations) throws DataAccessException {
            operations.multi();
            
            // Redis 트랜잭션으로 atomic 처리
            Long queueSize = operations.opsForList().size(queueKey);
            if (queueSize >= 3) {
                List<String> participants = operations.opsForList().rightPop(queueKey, 3);
                operations.exec();
                
                processMatchingWithParticipants(participants, worldType);
                return true;
            }
            
            operations.discard();
            return false;
        }
    });
}
```

## 성능 최적화

### 1. Redis 설정
- **큐 크기 제한**: 100명/세계관
- **TTL 설정**: 사용자 상태 1시간, 세션 정보 7일
- **Connection Pool**: Lettuce 기본 설정 활용

### 2. WebSocket 최적화
- **메시지 압축**: JSON 최소화
- **구독 관리**: 사용자별 개별 채널
- **Connection 제한**: 필요시 추가 예정

### 3. 모니터링 포인트
- 세계관별 평균 대기 시간
- 매칭 성공률 (완료/참가 비율)
- WebSocket 연결 수

## 설정값

| 항목 | 값 | 설명 |
|------|----|----- |
| 최대 큐 크기 | 100명/세계관 | 메모리 사용량 제한 |
| 사용자 상태 TTL | 1시간 | 비활성 사용자 자동 정리 |
| 세션 정보 TTL | 7일 | 매칭 히스토리 보관 |
| 매칭 인원 | 3명 | 고정값 |
| 지원 세계관 | 3개 | FANTASY, SF, MODERN |

## 향후 확장 계획

### 1순위 (필수 완료 후)
- 매칭 실패 처리 개선
- 대기 시간 예측 알고리즘
- 봇 매칭 시스템

### 2순위 (운영 안정화 후)  
- 레벨/스킬 기반 매칭
- 친구 초대 매칭
- 매칭 통계 대시보드

## 테스트 시나리오

### 기본 시나리오
1. 단일 사용자 매칭 참가/취소
2. 3명 동시 매칭 완료
3. 브라우저 새로고침 후 상태 복구

### 예외 시나리오  
1. 동일 사용자 중복 참가 시도
2. 매칭 완료 직후 접속 실패
3. 서버 재시작 시 큐 정리

### 성능 시나리오
1. 100명 동시 매칭 참가
2. 여러 세계관 동시 매칭 완료
3. 대량 WebSocket 연결/해제