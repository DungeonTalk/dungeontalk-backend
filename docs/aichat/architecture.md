# 🏗️ AI 채팅 시스템 아키텍처

## 시스템 개요

DungeonTalk AI Chat 시스템은 실시간 AI 채팅 기능을 제공하는 멀티플레이어 게임 시스템입니다.

## 📊 시스템 아키텍처

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Spring Boot   │    │   Python AI     │
│   (WebSocket)   │◄──►│   Backend       │◄──►│   Service       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                   ┌────────────┼────────────┐
                   │            │            │
          ┌─────────▼─────┐ ┌───▼───┐ ┌─────▼─────┐
          │   MongoDB     │ │ Redis │ │  WebSocket │
          │  (게임 데이터)  │ │(캐시)  │ │  Session   │
          └───────────────┘ └───────┘ └───────────┘
```

## 🔄 데이터 플로우

### 1. 게임방 생성 플로우
```
Client → REST API → GameRoomService → MongoDB
```

### 2. 실시간 채팅 플로우
```
Client → WebSocket STOMP → MessageService → MongoDB
                               ↓
                    Phase Change: TURN_INPUT → AI_RESPONSE
                               ↓
                    Redis Pub/Sub → All Connected Clients
```

### 3. AI 응답 생성 플로우
```
User Message → Phase: AI_RESPONSE → Python AI Service
                        ↓                    ↓
            Block All User Input    Generate Response
                        ↓                    ↓
                   MongoDB ← AI Response Processing
                        ↓
            Phase: AI_RESPONSE → TURN_INPUT
                        ↓
                Redis Pub/Sub → All Clients
```

## 🏛️ 계층 구조

### Presentation Layer
- **Controllers**: REST API 및 WebSocket STOMP 컨트롤러
- **DTOs**: 요청/응답 데이터 전송 객체

### Business Layer
- **Services**: 비즈니스 로직 처리
- **AI Integration**: Python AI 서비스 연동

### Data Access Layer
- **Repositories**: MongoDB 데이터 접근
- **Entities**: 데이터 모델

### Infrastructure Layer
- **Redis**: 캐싱 및 Pub/Sub
- **WebSocket**: 실시간 통신
- **HTTP Client**: 외부 서비스 연동

## 📦 주요 컴포넌트

### 1. Controller Layer
- `AiGameRoomController`: 게임방 CRUD API
- `AiResponseController`: AI 응답 생성 API
- `AiChatStompController`: WebSocket 메시징

### 2. Service Layer
- `AiGameRoomService`: 게임방 관리
- `AiGameStateService`: 게임 상태 제어
- `AiMessageService`: 메시지 관리
- `AiResponseService`: AI 서비스 연동

### 3. Data Layer
- `AiGameRoom`: 게임방 엔티티
- `AiMessage`: 메시지 엔티티
- `AiGameRoomRepository`: 데이터 접근

### 4. Infrastructure
- `ValkeyService`: Redis 연산
- `HttpClientConfig`: HTTP 클라이언트 설정
- `WebSocketConfig`: WebSocket 설정

## 🔐 보안 아키텍처

### JWT 인증
```
Request → JWT Filter → Token Validation → Controller
```

### 권한 제어
- 게임방 생성자만 시작/종료 권한
- WebSocket 연결 시 JWT 검증
- 메시지 전송 권한 확인

## 🌐 통신 프로토콜

### REST API
- HTTP/HTTPS 기반
- JSON 데이터 형식
- RESTful 설계 원칙

### WebSocket STOMP
- 실시간 양방향 통신
- 주제 기반 메시징
- 자동 재연결 지원

### AI Service Integration
- HTTP POST 요청
- JSON 페이로드
- 타임아웃 설정 (30초)

## 📈 확장성 고려사항

### 수평 확장
- Redis Cluster 지원
- MongoDB Replica Set
- Load Balancer 적용

### 성능 최적화
- Redis 캐싱 전략
- 비동기 메시지 처리
- Connection Pooling

### 모니터링
- 응답 시간 측정
- 에러 로깅
- 리소스 사용량 추적