# TRPG 게임 통합 작업 계획

## 📋 개요
`dungeon-game.html`의 TRPG 게임 기능을 `game.html`의 세계관 선택 후 실행되도록 통합

## 🎯 주요 목표
1. game.html에서 세계관 선택 시 TRPG 게임 시작
2. 15분 타이머 시스템 통합
3. AI 게임 마스터 & 플레이어 채팅 통합
4. 캐릭터 시스템 연동

## ✅ 작업 목록

### 1. 타이머 시스템 통합
- [ ] 15분 TRPG 타이머 컴포넌트를 game.html에 추가
- [ ] 게임 단계별 압박감 표시 (여유 → 보통 → 긴급 → 위급)
- [ ] 타이머 진행률 바 구현
- [ ] 게임 종료 시 타이머 정지 로직

### 2. 캐릭터 시스템 연동
- [ ] 캐릭터 생성 모달 통합
- [ ] 캐릭터 정보 표시 패널 추가
- [ ] 종족 선택 기능 구현
- [ ] 캐릭터 스탯 표시 (STR, WIL, INT, WIS, DEX, LUK)

### 3. WebSocket 통신 통합
- [ ] AI 게임방 WebSocket 연결 (`/ws-chat`)
- [ ] 매칭 WebSocket 연결 (`/ws-matching`)
- [ ] 파티 채팅 WebSocket 연결
- [ ] 메시지 타입별 처리 (AI, USER, SYSTEM, GAME_END)

### 4. UI/UX 개선
- [ ] Alpine.js 기반 상태 관리로 전환
- [ ] Tailwind CSS 스타일 적용
- [ ] 반응형 디자인 최적화
- [ ] 다크모드 지원

### 5. 게임 플로우 구현
- [ ] 세계관 선택 → 캐릭터 체크 → 매칭 시작
- [ ] 매칭 완료 → 게임 세션 생성
- [ ] AI 게임방 생성 및 초기화
- [ ] 게임 진행 중 상태 관리
- [ ] 게임 종료 처리 (성공/실패/시간초과)

### 6. 세계관별 테마 적용
- [ ] FANTASY: 중세 판타지 테마
- [ ] SCIFI: 사이버펑크 테마  
- [ ] MODERN: 현대 미스터리 테마
- [ ] 세계관별 환영 메시지 및 플레이스홀더

### 7. 채팅 시스템 개선
- [ ] AI 채팅과 플레이어 채팅 탭 분리
- [ ] 메시지 포맷팅 (마크다운 지원)
- [ ] 시스템 메시지 스타일링
- [ ] 자동 스크롤 기능

### 8. 게임 종료 처리
- [ ] 게임 결과 분석 (성공/실패/시간초과)
- [ ] 종료 애니메이션 효과
- [ ] 경험치 보상 처리
- [ ] 게임 통계 저장

## 🔧 기술 스택 마이그레이션

### 현재 (dungeon-game.html)
- Vanilla JavaScript
- Inline CSS
- SockJS + Stomp.js
- localStorage 기반 인증

### 목표 (game.html)
- Alpine.js
- Tailwind CSS
- Thymeleaf 템플릿
- 쿠키 & localStorage 하이브리드 인증

## 📝 주요 변경사항

### API 엔드포인트
```javascript
// 캐릭터 관련
GET  /v1/character/{memberId}
POST /v1/character
PUT  /v1/character/{memberId}/race

// 매칭 관련  
POST /v1/match/join
DELETE /v1/match/cancel

// AI 게임방
POST /v1/rooms/ai/{roomId}/start
```

### WebSocket 구독 경로
```javascript
// 매칭 알림
/sub/matching/user/{userId}

// AI 채팅
/sub/aichat/room/{roomId}

// 파티 채팅
/sub/chat/room/{roomId}
```

## 🚀 구현 우선순위

1. **Phase 1: 핵심 기능 통합**
   - 세계관 선택 후 게임 시작 플로우
   - WebSocket 연결 및 메시지 처리
   - 기본 채팅 기능

2. **Phase 2: 게임 메커니즘**
   - 15분 타이머 시스템
   - 게임 단계별 진행
   - AI 응답 처리

3. **Phase 3: UI/UX 개선**
   - Alpine.js 상태 관리
   - Tailwind 스타일링
   - 애니메이션 효과

4. **Phase 4: 캐릭터 시스템**
   - 캐릭터 생성/관리
   - 스탯 표시
   - 종족 변경

## 📌 주의사항

- 인증 토큰 관리 (JWT Bearer)
- WebSocket 연결 해제 처리
- 에러 핸들링 및 재연결 로직
- 중복 메시지 필터링
- 게임 세션 상태 동기화

## 🎮 테스트 시나리오

1. 로그인 → 세계관 선택 → 캐릭터 확인
2. 매칭 시작 → 대기 → 매칭 완료
3. 게임 시작 → AI와 상호작용 → 플레이어 채팅
4. 15분 경과 또는 목표 달성 → 게임 종료
5. 결과 확인 → 보상 수령

## 📅 예상 작업 시간

- 타이머 시스템: 2시간
- WebSocket 통합: 3시간
- UI/UX 마이그레이션: 4시간
- 캐릭터 시스템: 2시간
- 테스트 및 디버깅: 3시간

**총 예상 시간: 14시간**