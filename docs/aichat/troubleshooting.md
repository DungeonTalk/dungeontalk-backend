# 🚨 AI 채팅 시스템 문제 해결 가이드

## 일반적인 문제 해결

### 1. WebSocket 연결 실패

#### 증상
- 클라이언트에서 WebSocket 연결이 되지 않음
- "Connection failed" 오류 메시지

#### 원인 및 해결방법

**JWT 토큰 문제**
```javascript
// 문제: 토큰이 없거나 만료됨
Error: STOMP connection failed: Authentication failed

// 해결: 유효한 토큰 확인
const token = localStorage.getItem('jwt_token');
if (!token || isTokenExpired(token)) {
    refreshToken(); // 토큰 갱신
}
```

**CORS 설정 문제**
```java
// application.yml에서 CORS 설정 확인
websocket:
  cors:
    allowed-origins: 
      - "http://localhost:3000"
      - "https://your-domain.com"
```

**서버 상태 확인**
```bash
# WebSocket 엔드포인트 상태 확인
curl -I http://localhost:8080/ws
# 응답: HTTP/1.1 400 Bad Request (정상 - WebSocket 업그레이드 필요)
```

### 2. AI 응답 생성 실패

#### 증상
- AI 응답 API 호출 시 타임아웃 발생
- "AI 서비스에 연결할 수 없습니다" 오류

#### 해결방법

**Python AI 서비스 상태 확인**
```bash
# AI 서비스 헬스체크
curl http://localhost:8001/health

# 응답이 없으면 AI 서비스 재시작
docker restart ai-service
```

**타임아웃 설정 조정**
```yaml
# application.yml
ai-service:
  timeout: 60000  # 30초 → 60초로 증가
```

**로그 확인**
```bash
# AI 서비스 관련 로그 확인
grep -n "AiResponseService" logs/application.log | tail -20
```

### 3. 메시지 전송 실패

#### 증상
- 메시지 전송 후 다른 사용자에게 보이지 않음
- "메시지 전송에 실패했습니다" 오류

#### 해결방법

**Redis Pub/Sub 상태 확인**
```bash
# Redis 연결 상태 확인
redis-cli ping
# 응답: PONG

# Pub/Sub 채널 확인
redis-cli
> PUBSUB CHANNELS aichat:*
```

**게임 상태 확인**
```bash
# MongoDB에서 게임 상태 확인
mongo dungeondb
> db.ai_game_rooms.find({_id: "room-id"}).pretty()
```

### 4. 게임방 생성 실패

#### 증상
- 게임방 생성 시 409 Conflict 오류
- "이미 진행 중인 게임이 있습니다" 메시지

#### 해결방법

**중복 게임 확인**
```javascript
// 기존 게임방 확인 후 생성
async function createGameRoomSafe(gameId) {
    // 1. 기존 ACTIVE 게임방 확인
    const existingRooms = await fetch(`/api/v1/aichat/rooms/search?gameId=${gameId}&status=ACTIVE`);
    
    if (existingRooms.length > 0) {
        throw new Error('이미 진행 중인 게임이 있습니다');
    }
    
    // 2. 새 게임방 생성
    return createGameRoom(gameId);
}
```

## 🔍 로그 분석

### 주요 로그 패턴

#### 1. WebSocket 연결 로그
```
2025-08-08 01:00:00 [WebSocket-Handler] INFO  - STOMP CONNECT: user123 connected to session abc123
2025-08-08 01:00:01 [WebSocket-Handler] INFO  - STOMP SUBSCRIBE: /sub/aichat/room/room-id
2025-08-08 01:00:02 [WebSocket-Handler] INFO  - STOMP SEND: /pub/aichat/join from user123
```

#### 2. AI 응답 생성 로그
```
2025-08-08 01:01:00 [AI-Service] INFO  - AI 서비스 호출 시작 - Room: room-id
2025-08-08 01:01:02 [AI-Service] INFO  - AI 응답 수신 완료 - Room: room-id, 처리시간: 1500ms
2025-08-08 01:01:02 [Redis-Pub] INFO  - 메시지 발행 완료 - Channel: aichat:room-id
```

#### 3. 에러 로그 패턴
```
2025-08-08 01:02:00 [AI-Service] ERROR - AI 서비스 연결 실패 - Room: room-id
java.net.SocketTimeoutException: Read timed out
    at AiResponseService.generateAiResponse(AiResponseService.java:85)
```

### 로그 레벨별 확인 방법

**DEBUG 로그 활성화**
```yaml
# application.yml
logging:
  level:
    org.com.dungeontalk.domain.aichat: DEBUG
    org.springframework.web.socket: DEBUG
```

**특정 기간 로그 검색**
```bash
# 최근 1시간 에러 로그
grep "ERROR" logs/application.log | grep "$(date '+%Y-%m-%d %H')"

# AI 서비스 관련 로그
grep -E "(AiResponseService|AiServiceClient)" logs/application.log | tail -50
```

## 🚀 성능 이슈 진단

### 1. 응답 시간 지연

#### 모니터링 쿼리
```bash
# 평균 응답 시간 확인 (최근 1시간)
grep "AI 응답 수신 완료" logs/application.log | grep "$(date '+%Y-%m-%d %H')" | \
awk '{print $NF}' | sed 's/ms//' | awk '{sum+=$1; count++} END {print "평균:", sum/count "ms"}'
```

#### 해결방법
```yaml
# 타임아웃 및 연결 풀 최적화
ai-service:
  timeout: 45000
  connection-pool:
    max-total: 50
    max-per-route: 20
```

### 2. 메모리 사용량 증가

#### 메모리 사용량 확인
```bash
# JVM 힙 메모리 상태
jstat -gc <PID>

# 메모리 덤프 생성
jcmd <PID> GC.run_finalization
jcmd <PID> VM.gc
```

#### 해결방법
```bash
# JVM 메모리 옵션 최적화
java -Xms1g -Xmx4g -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -jar app.jar
```

### 3. 동시 접속자 처리

#### 커넥션 풀 모니터링
```java
// Actuator 엔드포인트로 확인
GET /actuator/metrics/hikaricp.connections.active
GET /actuator/metrics/hikaricp.connections.max
```

#### 최적화 설정
```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 30000
```

## 🔧 개발 환경 문제 해결

### 1. 로컬 개발 환경 설정

#### MongoDB 연결 실패
```bash
# MongoDB 서비스 시작
brew services start mongodb/brew/mongodb-community
# 또는
docker run -d -p 27017:27017 --name mongodb mongo:7.0
```

#### Redis(Valkey) 연결 실패
```bash
# Redis 서비스 시작
brew services start redis
# 또는
docker run -d -p 6379:6379 --name redis redis:7.2
```

### 2. IDE 설정 문제

#### IntelliJ IDEA 설정
```properties
# .idea/workspace.xml
# Spring Boot DevTools 설정
spring.devtools.restart.enabled=true
spring.devtools.livereload.enabled=true

# 자동 빌드 활성화
compiler.automake.allow.when.app.running=true
```

#### VS Code 설정
```json
// .vscode/settings.json
{
    "java.configuration.updateBuildConfiguration": "automatic",
    "spring-boot.ls.problem.application-properties.unknown-property": "ignore"
}
```

## 🛠️ 운영 환경 문제 해결

### 1. 메모리 부족 (OOM)

#### 증상 확인
```bash
# 메모리 사용량 확인
free -m
ps aux | grep java | head -5

# OOM 로그 확인
grep -i "OutOfMemoryError" /var/log/syslog
```

#### 해결방법
```bash
# JVM 힙 크기 증가
export JAVA_OPTS="-Xmx4g -XX:+HeapDumpOnOutOfMemoryError"

# 스왑 메모리 추가 (임시 해결)
sudo dd if=/dev/zero of=/swapfile bs=1024 count=2097152
sudo mkswap /swapfile
sudo swapon /swapfile
```

### 2. 데이터베이스 연결 문제

#### MongoDB 연결 풀 고갈
```bash
# 현재 연결 수 확인
mongo --eval "db.runCommand({serverStatus: 1}).connections"

# 연결 상태 확인
mongo --eval "db.runCommand({currentOp: true})"
```

#### 해결방법
```yaml
# MongoDB 연결 풀 설정 최적화
spring:
  data:
    mongodb:
      options:
        max-pool-size: 100
        min-pool-size: 10
        max-idle-time: 30000
```

### 3. 네트워크 연결 문제

#### 외부 서비스 연결 확인
```bash
# AI 서비스 연결 테스트
curl -v http://ai-service:8001/health
telnet ai-service 8001

# DNS 해결 확인
nslookup ai-service
```

#### 방화벽 설정 확인
```bash
# 포트 열림 확인
netstat -tlnp | grep 8080
ss -tlnp | grep 8080

# 방화벽 규칙 확인
sudo iptables -L | grep 8080
```

## 📊 모니터링 및 알림

### 핵심 메트릭 확인

#### 시스템 메트릭
```bash
# CPU 사용률
top -p $(pgrep java)

# 메모리 사용률
cat /proc/$(pgrep java)/status | grep Vm

# 네트워크 연결 수
ss -s | grep TCP
```

#### 애플리케이션 메트릭
```bash
# Actuator 엔드포인트
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/metrics/jvm.memory.used
curl http://localhost:8080/actuator/metrics/http.server.requests
```

### 알림 규칙 설정

#### Prometheus AlertManager 규칙
```yaml
# alert.rules
groups:
- name: aichat.rules
  rules:
  - alert: AiServiceDown
    expr: up{job="ai-service"} == 0
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "AI 서비스가 다운되었습니다"
      
  - alert: HighResponseTime
    expr: histogram_quantile(0.95, http_request_duration_seconds_bucket{job="aichat"}) > 5
    for: 2m
    labels:
      severity: warning
    annotations:
      summary: "응답 시간이 5초를 초과했습니다"
```

## 🔄 자동 복구 스크립트

### 서비스 재시작 스크립트
```bash
#!/bin/bash
# restart-aichat.sh

SERVICE_NAME="aichat-backend"
LOG_FILE="/var/log/aichat/restart.log"

echo "$(date): 서비스 상태 확인 중..." >> $LOG_FILE

# 서비스 상태 확인
if ! systemctl is-active --quiet $SERVICE_NAME; then
    echo "$(date): 서비스 다운 감지, 재시작 중..." >> $LOG_FILE
    
    # 서비스 재시작
    sudo systemctl restart $SERVICE_NAME
    
    # 재시작 확인
    sleep 10
    if systemctl is-active --quiet $SERVICE_NAME; then
        echo "$(date): 서비스 재시작 완료" >> $LOG_FILE
        
        # Slack 알림
        curl -X POST -H 'Content-type: application/json' \
             --data '{"text":"✅ AI Chat 서비스가 자동으로 재시작되었습니다"}' \
             $SLACK_WEBHOOK_URL
    else
        echo "$(date): 서비스 재시작 실패" >> $LOG_FILE
        
        # 긴급 알림
        curl -X POST -H 'Content-type: application/json' \
             --data '{"text":"🚨 AI Chat 서비스 재시작 실패! 수동 확인 필요"}' \
             $SLACK_WEBHOOK_URL
    fi
fi
```

### 디스크 공간 정리 스크립트
```bash
#!/bin/bash
# cleanup-logs.sh

LOG_DIR="/var/log/aichat"
RETENTION_DAYS=7

echo "$(date): 로그 정리 시작..."

# 7일 이상된 로그 파일 삭제
find $LOG_DIR -name "*.log" -type f -mtime +$RETENTION_DAYS -delete

# 압축된 로그 파일 삭제 (30일 이상)
find $LOG_DIR -name "*.gz" -type f -mtime +30 -delete

echo "$(date): 로그 정리 완료"
```