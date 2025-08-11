# 🚨 매칭 시스템 게임 시작 오류 해결 보고서

## 📋 문제 상황

매칭 완료 후 3명의 플레이어가 동시에 게임을 시작할 때 발생하는 문제

### 🔍 증상
- 첫 번째 플레이어: 정상적으로 게임 시작
- 나머지 2명: `401 에러` 발생 및 WebSocket 연결 끊김
- 콘솔 로그: `AiChatException` 및 JWT 인증 필터 오류

### 📊 에러 로그
```
🎮 게임방 상태가 잘못됨: roomId=..., status=ACTIVE, expected=CREATED
JWT 인증[필터] 중 오류 발생: AiChatException
WebSocket 연결 끊김 감지 → memberId: ...
```

## 🔍 원인 분석

### 1. **게임방 중복 시작 요청 문제**
- 매칭 완료 후 3명이 동시에 `/api/v1/aichat/rooms/{roomId}/start` API 호출
- 첫 번째 요청: 성공 (게임방 상태 `CREATED` → `ACTIVE`)
- 나머지 요청: 이미 `ACTIVE` 상태라서 예외 발생

### 2. **서버 측 상태 검증 로직**
```java
// 문제가 된 코드 (AiGameStateService.java:48-52)
if (room.getStatus() != AiGameStatus.CREATED) {
    log.warn("🎮 게임방 상태가 잘못됨: roomId={}, status={}, expected=CREATED", 
             aiGameRoomId, room.getStatus());
    throw new AiChatException(ErrorCode.AI_GAME_ROOM_INVALID_STATE);
}
```

### 3. **클라이언트 측 중복 요청**
- 매칭 완료 이벤트 수신 시 중복 방지 로직 없음
- 동일한 게임방에 대해 여러 번 시작 요청

## 🔧 해결 방안

### 1. **서버 측 수정** (AiGameStateService.java)

**Before:**
```java
if (room.getStatus() != AiGameStatus.CREATED) {
    throw new AiChatException(ErrorCode.AI_GAME_ROOM_INVALID_STATE);
}
```

**After:**
```java
// 이미 활성화된 게임방은 성공으로 처리
if (room.getStatus() == AiGameStatus.ACTIVE) {
    log.info("🎮 게임방이 이미 활성화됨: roomId={}, status={}", 
             aiGameRoomId, room.getStatus());
    return AiGameRoomResponse.fromEntity(room);
}

// 다른 잘못된 상태만 예외 처리
if (room.getStatus() != AiGameStatus.CREATED) {
    log.warn("🎮 게임방 상태가 잘못됨: roomId={}, status={}, expected=CREATED", 
             aiGameRoomId, room.getStatus());
    throw new AiChatException(ErrorCode.AI_GAME_ROOM_INVALID_STATE);
}
```

### 2. **클라이언트 측 수정** (dungeon-game.html)

#### 2.1 중복 요청 방지 플래그 추가
```javascript
// 전역 변수 추가
let gameSessionStarted = false;
```

#### 2.2 게임 시작 함수 수정
```javascript
async function startGameSession(matchData) {
    try {
        // 중복 요청 방지
        if (gameSessionStarted) {
            console.log('🔄 게임 세션이 이미 시작됨, 중복 요청 무시');
            return;
        }
        
        // JWT 토큰 유효성 검사
        if (!authToken || authToken === 'null' || authToken.trim() === '') {
            console.error('❌ JWT 토큰이 없거나 유효하지 않음:', authToken);
            throw new Error('인증 토큰이 필요합니다. 다시 로그인해주세요.');
        }
        
        gameSessionStarted = true;
        // ... API 호출 로직
    } catch (error) {
        gameSessionStarted = false; // 에러 시 플래그 리셋
        // ... 에러 처리
    }
}
```

#### 2.3 상태 초기화 함수 수정
```javascript
function clearAuthData() {
    authToken = null;
    currentUser = null;
    selectedWorld = null;
    gameSessionStarted = false; // 플래그 리셋 추가
    // ... 기타 초기화
}
```

#### 2.4 에러 처리 개선
```javascript
// 401 에러 시 자동 재로그인 유도
if (sessionResponse.status === 401) {
    alert('인증이 만료되었습니다. 다시 로그인해주세요.');
    clearAuthData();
    showLoginModal();
}
```

## ✅ 해결 결과

### 🎯 개선 효과
1. **중복 요청 방지**: 클라이언트에서 첫 요청만 처리, 나머지는 무시
2. **서버 안정성 향상**: 이미 활성화된 게임방은 정상 응답으로 처리
3. **사용자 경험 개선**: 3명 모두 정상적으로 게임 참여 가능
4. **에러 처리 강화**: JWT 토큰 문제 시 자동 재로그인 유도

### 🔄 동작 시나리오
1. **매칭 완료** → 3명에게 동시에 이벤트 전송
2. **첫 번째 사용자**: API 호출 → 게임방 생성 성공
3. **나머지 사용자**: API 호출 → 이미 생성된 게임방 정보 반환 (성공)
4. **모든 사용자**: WebSocket 연결 유지 → 정상적으로 게임 시작

## 🚀 배포 방법

1. **서버 재시작** 필요
2. **데이터베이스** 변경사항 없음
3. **클라이언트 캐시** 새로고침 권장

## 📝 향후 개선 사항

1. **게임방 상태 관리**: 더 세밀한 상태 전환 로직 구현
2. **동시성 제어**: Redis 분산 락을 이용한 동시 요청 처리
3. **모니터링**: 게임 시작 실패율 및 응답시간 추적
4. **테스트**: 동시 접속 시나리오 자동화 테스트 추가

---
*수정일: 2025-08-11*  
*작성자: Claude*