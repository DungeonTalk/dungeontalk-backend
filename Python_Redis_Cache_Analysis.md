# 던전톡 MVP - Python Redis 캐시 사용 분석 보고서

## 📋 개요
던전톡 MVP에서 Python AI 서비스가 Redis(Valkey)를 활용하여 AI 응답을 캐싱하는 방식을 분석한 자료입니다.

---

## 🔧 1. Redis 연결 설정 및 구성

### 1.1 환경변수 설정
```env
REDIS_CACHE_HOST=dgt-valkey-cache
REDIS_CACHE_PORT=6379
CACHE_TTL_MINUTES=30
```

### 1.2 Redis 클라이언트 초기화
```python
self.redis_client = redis.Redis(
    host=redis_host, 
    port=redis_port, 
    decode_responses=False,  # pickle 사용
    socket_connect_timeout=5,
    socket_timeout=5
)
```

### 1.3 Docker 구성 (Valkey 사용)
```yaml
valkey-cache:
  image: valkey/valkey:latest
  container_name: dgt-valkey-cache
  ports:
    - "${VALKEY_CACHE_PORT}:6379"
```

---

## 🔑 2. 캐시 키 네이밍 규칙 및 패턴

### 2.1 캐시 키 생성 방식
```python
# 키 생성: 세계관 + 사용자 + 메시지 + 컨텍스트를 해시화
content = f"{world_type}:{user}:{message}:{context_hash}"
key_hash = hashlib.md5(content.encode()).hexdigest()
return f"ai_response:{key_hash}"
```

### 2.2 캐시 키 패턴
- **구조**: `ai_response:{MD5_HASH}`
- **해시 원본**: `{세계관}:{사용자}:{메시지}:{컨텍스트해시}`
- **예시**: `ai_response:a1b2c3d4e5f6g7h8...`

---

## 💾 3. 캐시 저장/조회 로직

### 3.1 조회 방식
```python
# 최근 3개 메시지만 컨텍스트로 사용
recent_context = context_messages[-3:]
cached_data = self.redis_client.get(key)
if cached_data:
    return pickle.loads(cached_data)
```

### 3.2 저장 방식
```python
# TTL과 함께 저장
cached_data = pickle.dumps(response)
self.redis_client.setex(key, self.ttl_seconds, cached_data)
```

---

## ⏰ 4. TTL 및 데이터 구조

### 4.1 TTL 설정
- **기본값**: 30분 (1800초)
- **환경변수**: `CACHE_TTL_MINUTES=30`
- **적용**: `setex(key, ttl_seconds, data)`

### 4.2 캐시된 데이터 구조
- **직렬화**: Python pickle (Binary)
- **주요 필드**: AI 응답 내용, 응답시간, 세계관 타입, 게임 시간 정보

---

## 🔄 5. 캐시 무효화 전략

### 5.1 선택적 캐싱
```python
# 게임 종료 메시지는 캐싱하지 않음
if not current_message.lower().strip().endswith('[game_end]'):
    cached_response = self.response_cache.get(...)
```

### 5.2 무효화 조건
- **게임 종료 메시지**: `[GAME_END]` 포함 시 캐싱 제외
- **TTL 만료**: 30분 후 자동 만료
- **컨텍스트 변경**: 최근 3개 메시지 변경 시 새 키 생성

### 5.3 폴백 메커니즘
- **Redis 장애 시**: 메모리 기반 임시 캐시 (최대 100개)
- **LRU 방식**: 오래된 캐시 자동 삭제

---

## 🎯 6. 주요 기능 및 설정

### 6.1 Valkey 서버 설정
```conf
maxmemory 256mb
maxmemory-policy allkeys-lru
appendonly no  # 영속성 비활성화 (캐시 용도)
```

### 6.2 캐시 통계 API
```python
@app.get("/cache-stats")
async def get_cache_stats():
    stats = enhanced_rag.response_cache.get_stats()
    return {"success": True, "stats": stats}
```

---

## ⚡ 7. 성능 최적화 및 효과

### 7.1 최적화 기법
- **컨텍스트 제한**: 최근 3개 메시지만 해시에 포함
- **연결 타임아웃**: 5초 설정으로 빠른 실패 처리
- **메모리 효율**: 256MB 제한, LRU 정책

### 7.2 주요 효과
- **응답 속도**: 캐시 히트 시 즉시 응답
- **비용 절약**: 중복 AI API 호출 방지
- **안정성**: Redis 장애 시 메모리 폴백

---

## 🔍 8. 결론

던전톡 MVP의 **Redis 캐시 시스템 특징:**

🎯 **스마트한 캐싱**
- 세계관+사용자+메시지+컨텍스트 기반 해시 키
- 게임 종료 메시지 등 선택적 캐싱

⚡ **성능 최적화**  
- 30분 TTL, 최근 3개 메시지만 컨텍스트 사용
- Redis 장애 시 메모리 폴백

🛡️ **안정성 확보**
- 타임아웃 설정, LRU 정책
- AI API 비용 절감 및 응답 속도 향상

**→ AI 기반 TRPG 게임의 응답성과 안정성을 크게 향상시킴**