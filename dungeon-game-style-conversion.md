# Dungeon Game Modern Fantasy 스타일 변환 Task

## 목표
`dungeon-game.html`의 모든 기능을 유지하면서 `dungeon-game-modern-fantasy.html`의 현대적인 스타일과 테마를 적용하여 새로운 HTML 파일을 생성한다.

## 🎨 Modern Fantasy 스타일 분석 (완료)

### 추출된 핵심 스타일 요소

#### 1. CSS 변수 시스템
```css
:root {
    /* 골드/황금색 판타지 색상 */
    --primary: #d4af37;
    --primary-light: #f4d03f;
    --primary-dark: #b8941f;
    --accent: #ffd700;
    --accent-light: #ffed4e;
    --accent-dark: #e6c200;
    --magic: #daa520;
    --magic-light: #f1c232;
    --magic-dark: #b8860b;
    
    /* 배경 그라디언트 */
    --bg-gradient-start: #1a1a0f;
    --bg-gradient-middle: #2e2a1a;
    --bg-gradient-end: #3e3416;
    
    /* 글래스 효과 */
    --glass: rgba(255, 255, 255, 0.1);
    --glass-dark: rgba(0, 0, 0, 0.2);
}
```

#### 2. 글래스모피즘 카드
```css
.glass-card {
    background: var(--glass);
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.2);
    border-radius: 20px;
    box-shadow: 
        0 8px 32px rgba(31, 38, 135, 0.15),
        inset 0 0 0 1px rgba(255, 255, 255, 0.1);
    transition: all 0.3s ease;
}
```

#### 3. 네오모피즘 버튼
```css
.neo-btn {
    background: linear-gradient(135deg, var(--primary), var(--primary-dark));
    border: none;
    border-radius: 12px;
    padding: 12px 28px;
    font-weight: 600;
    color: white;
    box-shadow: 
        0 4px 15px rgba(212, 175, 55, 0.3),
        inset 0 1px 0 rgba(255, 255, 255, 0.2);
}
```

#### 4. 모던 타이틀 애니메이션
- logoGlow: 빛나는 효과
- logoFloat: 부유 효과  
- logoBlur: 블러 글로우
- swordSwing: 아이콘 흔들림

#### 5. 파티클 애니메이션
- 10개의 황금색 파티클이 아래에서 위로 부유
- particleFloat 애니메이션 (10초 주기)

## 변환 요구사항

### 1. 유지해야 할 요소 (dungeon-game.html)
- ✅ 모든 JavaScript 로직 (vanilla JS 유지)
- ✅ WebSocket 통신 기능
- ✅ STOMP.js 기반 메시지 처리
- ✅ JWT 토큰 인증
- ✅ 채팅 기능
- ✅ 파티 시스템
- ✅ 던전 입장/퇴장 로직
- ✅ 디버깅 로그 (console.log)

### 2. 적용해야 할 스타일 요소 (dungeon-game-modern-fantasy.html)

#### 색상 테마
- 황금색 계열 그라데이션 (#FFD700, #FFA500, #FF8C00)
- 어두운 배경 (#0f0f23)
- Glassmorphism 효과
- Neumorphism 그림자 효과

#### 주요 CSS 스타일
```css
/* 골든 테마 색상 */
--primary-gold: #FFD700;
--secondary-gold: #FFA500;
--accent-gold: #FF8C00;
--text-light: #F5F5DC;
--bg-dark: #0f0f23;
--glass-bg: rgba(255, 215, 0, 0.1);

/* Glassmorphism 효과 */
backdrop-filter: blur(10px);
background: rgba(255, 215, 0, 0.05);
border: 1px solid rgba(255, 215, 0, 0.2);

/* Neumorphism 효과 */
box-shadow: 
  20px 20px 60px rgba(0, 0, 0, 0.5),
  -20px -20px 60px rgba(255, 215, 0, 0.1);

/* 호버 효과 */
transition: all 0.3s ease;
transform: translateY(-2px);
```

#### 애니메이션 요소
- 파티클 배경 애니메이션
- 부드러운 페이드 전환
- 호버 시 글로우 효과
- 타이핑 애니메이션 효과

#### UI 컴포넌트 스타일
- 모던한 카드 레이아웃
- 반투명 채팅 창
- 그라데이션 버튼
- 커스텀 스크롤바
- 애니메이션 로딩 인디케이터

### 3. 파일 구조 계획

```
dungeon-game-styled.html
├── <head>
│   ├── Modern Fantasy 스타일 시트
│   ├── 파티클 애니메이션 CSS
│   └── 폰트 및 아이콘
├── <body>
│   ├── 파티클 배경 캔버스
│   ├── 기존 dungeon-game 구조
│   │   ├── 로그인 폼 (스타일 업데이트)
│   │   ├── 파티 시스템 (스타일 업데이트)
│   │   └── 채팅 인터페이스 (스타일 업데이트)
│   └── 기존 JavaScript (변경 없음)
```

### 4. 작업 단계

#### Step 1: CSS 스타일 추출
- dungeon-game-modern-fantasy.html에서 모든 <style> 블록 추출
- 파티클 애니메이션 관련 스타일 추출
- 컴포넌트별 스타일 분류

#### Step 2: HTML 구조 매핑
- dungeon-game.html의 클래스명과 ID 확인
- modern-fantasy 스타일과 매칭될 수 있는 요소 식별
- 필요한 경우 래퍼 div 추가 계획

#### Step 3: 스타일 통합
- 기존 인라인 스타일 제거
- Modern Fantasy 스타일 적용
- 클래스명 조정 (필요시)

#### Step 4: 파티클 배경 추가
- 캔버스 요소 추가
- 파티클 애니메이션 JavaScript 통합
- z-index 조정으로 배경 처리

#### Step 5: 테스트 및 검증
- WebSocket 연결 테스트
- 채팅 기능 테스트
- 스타일 렌더링 확인
- 반응형 디자인 검증

### 5. 주의사항
- ⚠️ JavaScript 로직은 절대 변경하지 않음
- ⚠️ ID와 이벤트 리스너는 유지
- ⚠️ WebSocket 관련 코드 보존
- ⚠️ 디버깅 콘솔 로그 유지

### 6. 예상 결과
- 기존 dungeon-game.html의 모든 기능 동작
- Modern Fantasy의 시각적 테마 적용
- 부드러운 애니메이션과 인터랙션
- 향상된 사용자 경험

## 🔍 dungeon-game.html 구조 분석 (완료)

### 주요 HTML 요소 매핑
| 기존 요소 (dungeon-game.html) | 적용할 스타일 | 설명 |
|-------------------------------|--------------|------|
| `.header` | `.modern-title` + 애니메이션 | 헤더 타이틀 |
| `.game-status` | `.glass-card` | 게임 상태 카드 |
| `.login-section` | `.glass-card` | 로그인 섹션 |
| `.matching-section` | `.glass-card` | 매칭 섹션 |
| `.world-card` | `.world-card` (개선) | 세계관 선택 카드 |
| `.chat-panel` | `.chat-container` | 채팅 패널 |
| `.btn-*` | `.neo-btn` | 모든 버튼 |
| `.modal-content` | `.glass-card` | 모달 창 |
| `input` | `.modern-input` | 입력 필드 |

### JavaScript 보존 요소
- ✅ 모든 함수 (register, login, selectWorld 등)
- ✅ WebSocket 연결 (matchingStompClient, aiStompClient, partyStompClient)
- ✅ 이벤트 리스너 (onclick, onkeypress)
- ✅ ID 기반 요소 참조
- ✅ 타이머 시스템

## 📋 스타일 전환 계획 (수립 완료)

### Phase 1: 기본 설정
1. CSS 변수 추가 (골드 테마)
2. 폰트 임포트 (Poppins, Inter)
3. 배경 그라디언트 설정

### Phase 2: 컴포넌트 스타일링
1. 헤더 - 모던 타이틀 애니메이션
2. 카드 컴포넌트 - 글래스모피즘
3. 버튼 - 네오모피즘
4. 채팅 인터페이스 - 글래스 효과
5. 모달 - 글래스 카드

### Phase 3: 애니메이션
1. 파티클 배경 (선택)
2. 호버 효과
3. 메시지 슬라이드인
4. 로딩 애니메이션

### Phase 4: 최적화
1. 기존 기능 검증
2. 반응형 대응
3. 성능 최적화

## 실행 계획
1. ✅ 문서 작성 완료
2. ✅ 스타일 분석 완료
3. ✅ 구조 분석 완료
4. ✅ 전환 계획 수립
5. ⏳ dungeon-game-styled.html 생성
6. ⏳ 테스트 및 검증