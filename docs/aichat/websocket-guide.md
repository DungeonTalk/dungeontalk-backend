# ⚡ WebSocket Real-time Communication Guide

## 개요

AI Chat 시스템은 WebSocket STOMP 프로토콜을 사용하여 실시간 양방향 통신을 지원합니다.

## 🔌 연결 설정

### 1. SockJS + STOMP 연결
```javascript
// SockJS 소켓 생성
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

// 연결 설정
const connectHeaders = {
    'Authorization': 'Bearer ' + jwtToken
};

stompClient.connect(connectHeaders, onConnected, onError);
```

### 2. 연결 상태 관리
```javascript
function onConnected(frame) {
    console.log('WebSocket 연결 성공:', frame);
    
    // 게임방 구독
    subscribeToRoom(currentRoomId);
}

function onError(error) {
    console.error('WebSocket 연결 오류:', error);
    
    // 재연결 시도
    setTimeout(reconnect, 3000);
}

function reconnect() {
    if (stompClient && stompClient.connected) {
        stompClient.disconnect();
    }
    connect();
}
```

## 📡 구독 (Subscribe)

### 게임방별 메시지 구독
```javascript
function subscribeToRoom(roomId) {
    const subscription = stompClient.subscribe(
        '/sub/aichat/room/' + roomId,
        function(message) {
            handleMessage(JSON.parse(message.body));
        }
    );
    
    return subscription;
}
```

### 구독 해제
```javascript
function unsubscribeFromRoom(subscription) {
    if (subscription) {
        subscription.unsubscribe();
    }
}
```

## 📤 메시지 발행 (Publish)

### 1. 게임방 참여
```javascript
function joinRoom(roomId) {
    const joinMessage = {
        roomId: roomId,
        username: getCurrentUsername()
    };
    
    stompClient.send('/pub/aichat/join', {}, JSON.stringify(joinMessage));
}
```

### 2. 사용자 메시지 전송
```javascript
function sendUserMessage(roomId, content) {
    const message = {
        roomId: roomId,
        content: content,
        messageType: 'USER'
    };
    
    stompClient.send('/pub/aichat/send', {}, JSON.stringify(message));
}
```

## 📨 메시지 타입

### 1. 사용자 메시지
```json
{
  "type": "USER_MESSAGE",
  "roomId": "room-123",
  "messageId": "msg-456",
  "sender": "user123",
  "content": "던전으로 들어갑니다",
  "turnNumber": 1,
  "messageOrder": 1,
  "timestamp": "2025-08-08T01:00:00Z"
}
```

### 2. AI 응답 메시지
```json
{
  "type": "AI_RESPONSE", 
  "roomId": "room-123",
  "messageId": "msg-457",
  "sender": "AI",
  "content": "던전 입구에서 이상한 소리가 들립니다. 어둠 속에서 무언가가 움직이는 것 같습니다.",
  "turnNumber": 1,
  "messageOrder": 2,
  "aiSource": "gpt-4",
  "processingTime": 1500,
  "timestamp": "2025-08-08T01:00:30Z"
}
```

### 3. 시스템 메시지
```json
{
  "type": "SYSTEM",
  "roomId": "room-123",
  "content": "user456님이 게임방에 참여했습니다",
  "systemEvent": "USER_JOINED",
  "timestamp": "2025-08-08T01:01:00Z"
}
```

### 4. 게임 상태 변경
```json
{
  "type": "GAME_STATE_CHANGED",
  "roomId": "room-123",
  "previousState": "CREATED",
  "currentState": "ACTIVE",
  "phase": "TURN_INPUT",
  "turnNumber": 1,
  "timestamp": "2025-08-08T01:02:00Z"
}
```

## 🎮 게임 플로우

### 1. 게임방 참여 플로우
```javascript
async function joinGameRoom(roomId) {
    try {
        // 1. WebSocket 연결 확인
        if (!stompClient || !stompClient.connected) {
            await connect();
        }
        
        // 2. 게임방 구독
        const subscription = subscribeToRoom(roomId);
        
        // 3. 참여 메시지 발송
        joinRoom(roomId);
        
        return subscription;
    } catch (error) {
        console.error('게임방 참여 실패:', error);
        throw error;
    }
}
```

### 2. 메시지 처리 플로우
```javascript
function handleMessage(message) {
    switch (message.type) {
        case 'USER_MESSAGE':
            displayUserMessage(message);
            // 사용자 메시지 후 자동으로 AI_RESPONSE 페이즈로 전환
            updateGamePhase('AI_RESPONSE'); 
            disableMessageInput(); // AI 응답 중에는 입력 차단
            break;
            
        case 'AI_RESPONSE':
            displayAiMessage(message);
            updateGamePhase('TURN_INPUT'); // AI 응답 후 다시 입력 가능
            enableMessageInput();
            break;
            
        case 'SYSTEM':
            displaySystemMessage(message);
            break;
            
        case 'GAME_STATE_CHANGED':
            updateGameState(message);
            handlePhaseChange(message.phase);
            break;
            
        default:
            console.warn('Unknown message type:', message.type);
    }
}

function handlePhaseChange(newPhase) {
    switch (newPhase) {
        case 'WAITING':
            showWaitingMessage('다른 플레이어를 기다리는 중...');
            disableMessageInput();
            break;
            
        case 'TURN_INPUT':
            hideWaitingMessage();
            enableMessageInput();
            showInputPrompt('자유롭게 행동을 입력하세요');
            break;
            
        case 'AI_RESPONSE':
            disableMessageInput();
            showLoadingMessage('AI가 응답을 생성하고 있습니다...');
            break;
            
        case 'GAME_END':
            disableMessageInput();
            showGameEndMessage();
            break;
    }
}
```

## 🔐 인증 및 보안

### JWT 토큰 인증
```javascript
// 헤더에 JWT 토큰 포함
const headers = {
    'Authorization': 'Bearer ' + getJwtToken()
};

stompClient.connect(headers, onConnected, onError);
```

### 토큰 갱신
```javascript
function refreshToken() {
    // 토큰 갱신 API 호출
    fetch('/api/auth/refresh', {
        method: 'POST',
        headers: {
            'Authorization': 'Bearer ' + getRefreshToken()
        }
    })
    .then(response => response.json())
    .then(data => {
        updateJwtToken(data.accessToken);
        
        // WebSocket 재연결
        reconnectWithNewToken();
    });
}
```

## ⚠️ 에러 처리

### 연결 오류 처리
```javascript
function onError(error) {
    console.error('STOMP 에러:', error);
    
    // 에러 타입별 처리
    if (error.headers && error.headers.message) {
        const errorMessage = error.headers.message;
        
        switch (errorMessage) {
            case 'UNAUTHORIZED':
                handleAuthError();
                break;
                
            case 'ROOM_NOT_FOUND':
                handleRoomNotFound();
                break;
                
            default:
                handleGenericError(errorMessage);
        }
    }
}
```

### 메시지 전송 오류
```javascript
function sendMessageWithRetry(destination, message, maxRetries = 3) {
    let retries = 0;
    
    const send = () => {
        try {
            stompClient.send(destination, {}, JSON.stringify(message));
        } catch (error) {
            retries++;
            
            if (retries < maxRetries) {
                console.warn(`메시지 전송 실패, 재시도 ${retries}/${maxRetries}`);
                setTimeout(send, 1000 * retries);
            } else {
                console.error('메시지 전송 최종 실패:', error);
                throw error;
            }
        }
    };
    
    send();
}
```

## 📊 실시간 상태 관리

### 게임 상태 동기화
```javascript
class GameStateManager {
    constructor() {
        this.currentState = null;
        this.currentPhase = null;
        this.turnNumber = 0;
        this.participants = [];
    }
    
    updateFromMessage(message) {
        switch (message.type) {
            case 'GAME_STATE_CHANGED':
                this.currentState = message.currentState;
                this.currentPhase = message.phase;
                this.turnNumber = message.turnNumber;
                break;
                
            case 'USER_JOINED':
                if (!this.participants.includes(message.username)) {
                    this.participants.push(message.username);
                }
                break;
                
            case 'USER_LEFT':
                this.participants = this.participants.filter(
                    p => p !== message.username
                );
                break;
        }
        
        // UI 업데이트
        this.notifyStateChange();
    }
    
    notifyStateChange() {
        // UI 컴포넌트에 상태 변경 알림
        document.dispatchEvent(new CustomEvent('gameStateChanged', {
            detail: {
                state: this.currentState,
                phase: this.currentPhase,
                turnNumber: this.turnNumber,
                participants: this.participants
            }
        }));
    }
}
```

## 🔄 연결 복구 전략

### 자동 재연결
```javascript
class WebSocketManager {
    constructor() {
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 1000;
    }
    
    connect() {
        this.socket = new SockJS('/ws');
        this.stompClient = Stomp.over(this.socket);
        
        this.stompClient.connect(
            this.getHeaders(),
            this.onConnected.bind(this),
            this.onError.bind(this)
        );
    }
    
    onError(error) {
        console.error('WebSocket 오류:', error);
        
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.scheduleReconnect();
        } else {
            console.error('재연결 시도 횟수 초과');
            this.notifyConnectionFailed();
        }
    }
    
    scheduleReconnect() {
        this.reconnectAttempts++;
        const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);
        
        console.log(`${delay}ms 후 재연결 시도 (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
        
        setTimeout(() => {
            this.connect();
        }, delay);
    }
    
    onConnected(frame) {
        console.log('WebSocket 연결 복구됨');
        this.reconnectAttempts = 0;
        
        // 구독 복원
        this.restoreSubscriptions();
    }
}
```