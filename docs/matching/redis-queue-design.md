# Redis 큐 시스템 설계

## 개요

던전톡 매칭 시스템의 Redis 기반 큐 관리 시스템 상세 설계입니다.

## Redis 키 구조

### 1. 매칭 큐 (List 자료구조)

#### 기본 구조
```redis
# 세계관별 대기 큐 - FIFO 방식
matching:queue:FANTASY → ["user001", "user002", "user003"]
matching:queue:SF      → ["user004", "user005"] 
matching:queue:MODERN  → ["user006"]
```

#### 큐 관리 명령어
```redis
# 큐 참가 (왼쪽에 추가)
LPUSH matching:queue:FANTASY user007

# 매칭 완료 시 3명 추출 (오른쪽에서 제거)  
RPOP matching:queue:FANTASY 3

# 현재 대기 인원 조회
LLEN matching:queue:FANTASY

# 취소 시 특정 사용자 제거
LREM matching:queue:FANTASY 0 user007

# 전체 대기자 목록 조회 (디버깅용)
LRANGE matching:queue:FANTASY 0 -1
```

### 2. 사용자 매칭 상태 (Hash 자료구조)

#### 기본 구조
```redis
# 사용자별 매칭 정보 - TTL 1시간
matching:user:user001 → {
  "worldType": "FANTASY",
  "status": "WAITING",
  "joinedAt": "2024-01-15T10:30:00Z",
  "queuePosition": "2",
  "sessionId": "session-abc123"
}
```

#### 상태 관리 명령어
```redis
# 사용자 상태 저장
HMSET matching:user:user001 worldType FANTASY status WAITING joinedAt 2024-01-15T10:30:00Z
EXPIRE matching:user:user001 3600

# 사용자 상태 조회
HGETALL matching:user:user001

# 특정 필드만 조회
HGET matching:user:user001 status

# 사용자 상태 삭제 (매칭 완료/취소 시)
DEL matching:user:user001
```

### 3. 매칭 세션 정보 (Hash 자료구조)

#### 기본 구조
```redis  
# 매칭 완료 세션 - TTL 7일
matching:session:session123 → {
  "participants": "user001,user002,user003",
  "worldType": "FANTASY",
  "aiGameRoomId": "aigame-456", 
  "chatRoomId": "chat-789",
  "createdAt": "2024-01-15T10:35:00Z",
  "status": "ACTIVE"
}
```

#### 세션 관리 명령어
```redis
# 세션 정보 저장
HMSET matching:session:session123 participants "user001,user002,user003" worldType FANTASY
EXPIRE matching:session:session123 604800

# 세션 정보 조회
HGETALL matching:session:session123

# 활성 세션 검색 (사용자 기준)
KEYS matching:session:*
# 각 세션의 participants 필드 확인하여 사용자 포함 여부 확인
```

### 4. 큐 통계 정보 (Hash 자료구조)

#### 기본 구조
```redis
# 세계관별 실시간 통계
matching:stats:FANTASY → {
  "currentWaiting": "5",
  "todayMatches": "23", 
  "averageWaitTime": "45",
  "lastUpdated": "2024-01-15T10:45:00Z"
}
```

#### 통계 관리 명령어
```redis
# 통계 업데이트
HINCRBY matching:stats:FANTASY currentWaiting 1
HINCRBY matching:stats:FANTASY todayMatches 1

# 평균 대기 시간 업데이트  
HSET matching:stats:FANTASY averageWaitTime 50

# 통계 조회
HGETALL matching:stats:FANTASY
```

## 큐 관리 로직

### 1. 매칭 참가 프로세스

```java
public class MatchingQueueManager {
    
    // 1단계: 중복 참가 체크
    public boolean isUserInQueue(String userId) {
        return redisTemplate.hasKey("matching:user:" + userId);
    }
    
    // 2단계: 큐에 사용자 추가
    public void addToQueue(String userId, WorldType worldType) {
        String queueKey = "matching:queue:" + worldType.name();
        String userKey = "matching:user:" + userId;
        
        // 큐에 사용자 추가
        redisTemplate.opsForList().leftPush(queueKey, userId);
        
        // 사용자 상태 저장
        Map<String, String> userInfo = Map.of(
            "worldType", worldType.name(),
            "status", "WAITING", 
            "joinedAt", Instant.now().toString(),
            "sessionId", generateSessionId()
        );
        redisTemplate.opsForHash().putAll(userKey, userInfo);
        redisTemplate.expire(userKey, Duration.ofHours(1));
        
        // 통계 업데이트
        updateQueueStats(worldType, 1);
    }
    
    // 3단계: 매칭 가능 여부 확인
    public boolean canProcessMatching(WorldType worldType) {
        String queueKey = "matching:queue:" + worldType.name();
        Long queueSize = redisTemplate.opsForList().size(queueKey);
        return queueSize != null && queueSize >= 3;
    }
}
```

### 2. 매칭 완료 처리

```java
// 원자성 보장을 위한 Lua 스크립트 사용
private static final String EXTRACT_USERS_SCRIPT = """
    local queueKey = KEYS[1]
    local queueSize = redis.call('LLEN', queueKey)
    
    if queueSize < 3 then
        return nil
    end
    
    local users = {}
    for i = 1, 3 do
        local user = redis.call('RPOP', queueKey)
        if user then
            table.insert(users, user)
        end
    end
    
    return users
""";

public List<String> extractThreeUsers(WorldType worldType) {
    String queueKey = "matching:queue:" + worldType.name();
    
    // Lua 스크립트로 원자성 보장
    List<String> users = redisTemplate.execute(
        new DefaultRedisScript<>(EXTRACT_USERS_SCRIPT, List.class),
        Collections.singletonList(queueKey)
    );
    
    if (users != null && users.size() == 3) {
        // 사용자 상태를 MATCHED로 변경
        users.forEach(this::markUserAsMatched);
        
        // 통계 업데이트  
        updateQueueStats(worldType, -3);
        incrementMatchingStats(worldType);
        
        return users;
    }
    
    return Collections.emptyList();
}
```

### 3. 큐 정리 프로세스

```java
// 만료된 사용자 정리
@Scheduled(fixedRate = 300000) // 5분마다 실행
public void cleanupExpiredUsers() {
    for (WorldType worldType : WorldType.values()) {
        String queueKey = "matching:queue:" + worldType.name();
        
        List<String> queueUsers = redisTemplate.opsForList().range(queueKey, 0, -1);
        
        for (String userId : queueUsers) {
            String userKey = "matching:user:" + userId;
            
            // 사용자 상태 키가 존재하지 않으면 큐에서 제거
            if (!redisTemplate.hasKey(userKey)) {
                redisTemplate.opsForList().remove(queueKey, 0, userId);
                updateQueueStats(worldType, -1);
            }
        }
    }
}
```

## 동시성 처리

### 1. Redis 트랜잭션 활용

```java
public boolean processMatchingWithTransaction(WorldType worldType) {
    String queueKey = "matching:queue:" + worldType.name();
    String lockKey = "matching:lock:" + worldType.name();
    
    // 분산 락 획득 시도
    Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(
        lockKey, "locked", Duration.ofSeconds(10)
    );
    
    if (Boolean.TRUE.equals(lockAcquired)) {
        try {
            return redisTemplate.execute(new SessionCallback<Boolean>() {
                @Override
                public Boolean execute(RedisOperations operations) throws DataAccessException {
                    operations.multi();
                    
                    Long queueSize = operations.opsForList().size(queueKey);
                    if (queueSize != null && queueSize >= 3) {
                        // 3명 추출
                        for (int i = 0; i < 3; i++) {
                            operations.opsForList().rightPop(queueKey);
                        }
                        
                        operations.exec();
                        return true;
                    }
                    
                    operations.discard();
                    return false;
                }
            });
        } finally {
            // 락 해제
            redisTemplate.delete(lockKey);
        }
    }
    
    return false;
}
```

### 2. 큐 크기 제한

```java  
public void addToQueueWithLimit(String userId, WorldType worldType) {
    String queueKey = "matching:queue:" + worldType.name();
    
    // 현재 큐 크기 확인
    Long currentSize = redisTemplate.opsForList().size(queueKey);
    
    if (currentSize != null && currentSize >= MAX_QUEUE_SIZE) {
        throw new MatchingException("매칭 대기열이 가득 찼습니다. 잠시 후 다시 시도해주세요.");
    }
    
    // 큐에 추가
    redisTemplate.opsForList().leftPush(queueKey, userId);
}
```

## 모니터링 및 디버깅

### 1. 큐 상태 조회 명령어

```bash
# 전체 큐 현황
redis-cli LLEN matching:queue:FANTASY
redis-cli LLEN matching:queue:SF  
redis-cli LLEN matching:queue:MODERN

# 특정 큐의 대기자 목록
redis-cli LRANGE matching:queue:FANTASY 0 -1

# 사용자 상태 확인
redis-cli HGETALL matching:user:user001

# 통계 정보 확인
redis-cli HGETALL matching:stats:FANTASY
```

### 2. 큐 비우기 (긴급 상황)

```bash
# 특정 세계관 큐 초기화
redis-cli DEL matching:queue:FANTASY

# 모든 사용자 상태 삭제
redis-cli --scan --pattern "matching:user:*" | xargs redis-cli DEL

# 모든 매칭 관련 데이터 삭제 (위험!)
redis-cli --scan --pattern "matching:*" | xargs redis-cli DEL
```

### 3. 성능 모니터링

```java
// 큐 처리 성능 측정
@Component
public class QueueMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Timer matchingTimer;
    private final Counter matchingAttempts;
    
    public QueueMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.matchingTimer = Timer.builder("matching.processing.time")
            .description("Time taken to process matching")
            .register(meterRegistry);
            
        this.matchingAttempts = Counter.builder("matching.attempts")
            .description("Number of matching attempts")
            .register(meterRegistry);
    }
    
    public void recordMatchingTime(Duration duration) {
        matchingTimer.record(duration);
    }
    
    public void incrementMatchingAttempts() {
        matchingAttempts.increment();
    }
}
```

## 설정 및 튜닝

### Redis 설정 최적화

```properties
# application.properties
spring.redis.cache.host=localhost
spring.redis.cache.port=6379
spring.redis.cache.timeout=2000ms
spring.redis.cache.lettuce.pool.max-active=20
spring.redis.cache.lettuce.pool.max-idle=10
spring.redis.cache.lettuce.pool.min-idle=5

# 매칭 시스템 설정
matching.queue.max-size=100
matching.user.ttl=3600
matching.session.ttl=604800
matching.cleanup.interval=300000
```

### 메모리 사용량 예측

```
단일 사용자 상태: ~200 bytes
단일 큐 엔트리: ~20 bytes  
단일 세션 정보: ~300 bytes

100명 대기 시 예상 메모리:
- 큐 데이터: 100 * 20 = 2KB
- 사용자 상태: 100 * 200 = 20KB  
- 통계 정보: ~1KB
총합: 약 23KB (세계관당)

3개 세계관 운영 시: ~70KB
```

## 문제 해결 가이드

### 자주 발생하는 문제들

1. **큐에서 사용자가 빠지지 않음**
   - 원인: TTL 설정 누락 또는 cleanup 프로세스 미작동
   - 해결: `EXPIRE` 명령으로 TTL 재설정

2. **동시 매칭 시 중복 처리**
   - 원인: 트랜잭션 처리 누락
   - 해결: Lua 스크립트나 분산 락 적용

3. **큐 크기 불일치**  
   - 원인: 예외 상황에서 통계 업데이트 누락
   - 해결: 정기적인 통계 재계산 배치 작업

### 디버깅 도구

```bash
# 실시간 Redis 명령 모니터링
redis-cli MONITOR | grep matching

# 특정 패턴 키 검색
redis-cli --scan --pattern "matching:*"

# 메모리 사용량 확인
redis-cli INFO memory
```