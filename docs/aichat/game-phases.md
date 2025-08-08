# 🎯 AI 채팅 게임 페이즈 참조서

## 게임 페이즈 상세 설명

### WAITING
- **상태**: 플레이어들이 게임방에 입장하기를 기다리는 단계
- **조건**: 최소 참여 인원 미달 시
- **특징**: 
  - 게임 시작 전 상태
  - 메시지 입력 불가
  - 참여자 수가 조건을 만족하면 게임 시작 가능

### TURN_INPUT  
- **상태**: 플레이어들이 턴 입력을 하는 단계
- **조건**: 게임이 ACTIVE 상태이고 AI 응답 대기 중이 아닐 때
- **특징**:
  - 모든 플레이어가 자유롭게 메시지 입력 가능
  - 임의의 플레이어가 메시지를 전송하면 즉시 AI_RESPONSE로 전환
  - 턴 기반이 아닌 자유형 입력 방식

### AI_RESPONSE
- **상태**: AI가 응답을 생성하고 있는 단계  
- **조건**: Python AI 서비스에서 응답 생성 중
- **특징**:
  - 모든 플레이어의 메시지 전송이 차단됨
  - AI 응답 생성 완료 후 자동으로 TURN_INPUT으로 복귀
  - 분산 락을 통한 중복 요청 방지

### GAME_END
- **상태**: 게임이 종료된 상태
- **조건**: 
  - 플레이어가 게임 종료를 선택했거나
  - AI가 게임 종료를 선언했거나  
  - 에러로 인해 게임이 중단된 경우
- **특징**:
  - 모든 입력 차단
  - 게임 통계 표시
  - 리소스 정리

## 페이즈 전환 규칙

```
WAITING → TURN_INPUT (게임 시작 시)
TURN_INPUT → AI_RESPONSE (사용자 메시지 전송 시)
AI_RESPONSE → TURN_INPUT (AI 응답 완료 시)
TURN_INPUT → GAME_END (게임 종료 명령 시)
AI_RESPONSE → GAME_END (AI가 게임 종료 선언 시)
```

## 코드에서의 활용

### Enum 정의
```java
public enum AiGamePhase {
    WAITING,     // 플레이어 대기 중
    TURN_INPUT,  // 사용자 입력 가능
    AI_RESPONSE, // AI 응답 생성 중
    GAME_END;    // 게임 종료
}
```

### 페이즈 검증 예시
```java
private void validateTurnInput(AiGameRoom gameRoom, String userId) {
    if (gameRoom.getPhase() != GamePhase.TURN_INPUT) {
        throw new GameException("현재 입력을 받을 수 없습니다. AI가 응답 중입니다.");
    }
}
```

### 클라이언트 상태 관리
```javascript
function updateGamePhase(newPhase) {
    currentPhase = newPhase;
    
    switch (newPhase) {
        case 'TURN_INPUT':
            enableMessageInput();
            showStatus('자유롭게 행동을 입력하세요');
            break;
        case 'AI_RESPONSE':
            disableMessageInput(); 
            showStatus('AI가 응답을 생성하고 있습니다...');
            break;
        // ... 기타 페이즈 처리
    }
}
```