# 🚀 AI 채팅 API 가이드

## REST API 엔드포인트

### 🏠 게임방 관리 API

#### 1. 게임방 생성
```http
POST /api/v1/aichat/rooms
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "gameId": "string",
  "title": "string",
  "description": "string",
  "maxParticipants": 4
}
```

**Response (201 Created):**
```json
{
  "resultCode": "S-1",
  "message": "게임방이 성공적으로 생성되었습니다",
  "data": {
    "id": "66b4a1234567890abcdef123",
    "gameId": "my-game",
    "title": "AI 던전 탐험",
    "description": "AI와 함께하는 던전 탐험",
    "status": "CREATED",
    "maxParticipants": 4,
    "currentParticipants": 1,
    "createdBy": "user123",
    "createdAt": "2025-08-08T01:00:00Z"
  }
}
```

#### 2. 게임방 조회
```http
GET /api/v1/aichat/rooms/{roomId}
Authorization: Bearer {jwt_token}
```

**Response (200 OK):**
```json
{
  "resultCode": "S-1",
  "message": "게임방 정보를 성공적으로 조회했습니다",
  "data": {
    "id": "66b4a1234567890abcdef123",
    "gameId": "my-game",
    "title": "AI 던전 탐험",
    "status": "ACTIVE",
    "phase": "TURN_INPUT",
    "turnNumber": 3,
    "participants": ["user123", "user456"],
    "createdAt": "2025-08-08T01:00:00Z"
  }
}
```

#### 3. 게임 시작
```http
POST /api/v1/aichat/rooms/{roomId}/start
Authorization: Bearer {jwt_token}
```

**Response (200 OK):**
```json
{
  "resultCode": "S-1",
  "message": "게임이 성공적으로 시작되었습니다"
}
```

#### 4. 게임 종료
```http
POST /api/v1/aichat/rooms/{roomId}/end
Authorization: Bearer {jwt_token}
```

### 🤖 AI 응답 생성 API

#### AI 응답 요청
```http
POST /api/v1/aichat/ai-service/rooms/{roomId}/generate
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "message": "던전에서 보물상자를 발견했습니다!"
}
```

**Response (200 OK):**
```json
{
  "resultCode": "S-1",
  "message": "AI 응답이 성공적으로 생성되었습니다",
  "data": {
    "aiResponse": "보물상자를 열어보니 반짝이는 검이 들어있다...",
    "turnNumber": 4,
    "timestamp": "2025-08-08T01:05:00Z"
  }
}
```

## 🔌 WebSocket STOMP API

### 연결 설정
```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect(
    { Authorization: 'Bearer ' + jwtToken },
    function(frame) {
        console.log('Connected: ' + frame);
    }
);
```

### 구독 채널
```javascript
// 게임방별 메시지 수신
stompClient.subscribe('/sub/aichat/room/' + roomId, function(message) {
    const data = JSON.parse(message.body);
    console.log('Received:', data);
});
```

### 메시지 발행

#### 1. 게임방 참여
```javascript
stompClient.send('/pub/aichat/join', {}, JSON.stringify({
    roomId: 'room-id',
    username: 'user123'
}));
```

#### 2. 메시지 전송
```javascript
stompClient.send('/pub/aichat/send', {}, JSON.stringify({
    roomId: 'room-id',
    content: '안녕하세요!',
    messageType: 'USER'
}));
```

### 수신 메시지 형식

#### 사용자 메시지
```json
{
  "type": "USER_MESSAGE",
  "roomId": "room-id",
  "messageId": "msg-123",
  "sender": "user123",
  "content": "던전으로 들어갑니다",
  "turnNumber": 1,
  "timestamp": "2025-08-08T01:00:00Z"
}
```

#### AI 응답 메시지
```json
{
  "type": "AI_RESPONSE",
  "roomId": "room-id",
  "messageId": "msg-124",
  "sender": "AI",
  "content": "던전 입구에서 이상한 소리가 들립니다...",
  "turnNumber": 1,
  "timestamp": "2025-08-08T01:00:30Z"
}
```

#### 시스템 메시지
```json
{
  "type": "SYSTEM",
  "roomId": "room-id",
  "content": "user456님이 게임방에 참여했습니다",
  "timestamp": "2025-08-08T01:00:00Z"
}
```

## 📋 에러 코드

### HTTP 상태 코드

| 코드 | 설명 | 예시 |
|------|------|------|
| 200 | 성공 | 정상 처리 |
| 201 | 생성됨 | 게임방 생성 |
| 400 | 잘못된 요청 | 유효하지 않은 파라미터 |
| 401 | 인증 실패 | JWT 토큰 오류 |
| 403 | 권한 없음 | 게임방 접근 권한 없음 |
| 404 | 찾을 수 없음 | 존재하지 않는 게임방 |
| 409 | 충돌 | 이미 진행 중인 게임 |
| 500 | 서버 오류 | 내부 서버 오류 |

### 커스텀 에러 코드

```json
{
  "resultCode": "F-1",
  "message": "게임방을 찾을 수 없습니다",
  "data": null
}
```

| 코드 | 설명 |
|------|------|
| F-1 | 게임방을 찾을 수 없음 |
| F-2 | 게임 상태 오류 |
| F-3 | AI 서비스 연결 실패 |
| F-4 | 권한 없음 |
| F-5 | 중복 요청 |

## 🔧 클라이언트 구현 예시

### JavaScript 클라이언트
```javascript
class AiChatClient {
    constructor(jwtToken) {
        this.jwtToken = jwtToken;
        this.socket = null;
        this.stompClient = null;
    }
    
    connect() {
        this.socket = new SockJS('/ws');
        this.stompClient = Stomp.over(this.socket);
        
        this.stompClient.connect(
            { Authorization: 'Bearer ' + this.jwtToken },
            this.onConnected.bind(this),
            this.onError.bind(this)
        );
    }
    
    joinRoom(roomId) {
        this.stompClient.subscribe('/sub/aichat/room/' + roomId, 
            this.onMessageReceived.bind(this));
        
        this.stompClient.send('/pub/aichat/join', {}, JSON.stringify({
            roomId: roomId,
            username: 'current-user'
        }));
    }
    
    sendMessage(roomId, content) {
        this.stompClient.send('/pub/aichat/send', {}, JSON.stringify({
            roomId: roomId,
            content: content,
            messageType: 'USER'
        }));
    }
    
    onMessageReceived(message) {
        const data = JSON.parse(message.body);
        console.log('Message received:', data);
    }
}
```

## 🧪 테스트 도구

시스템에는 다음과 같은 테스트 HTML 페이지들이 제공됩니다:

- `/ai-chat-test.html`: 완전한 AI 채팅 테스트 도구
- `/ai-chat-simple-test.html`: 간단한 테스트 인터페이스
- `/login-test.html`: JWT 토큰 생성 도구