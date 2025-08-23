# Thymeleaf 관련 파일 기록

## 개요
이 문서는 DungeonTalk 백엔드에서 사용되었던 Thymeleaf 템플릿 파일들과 관련 JavaScript, CSS 파일들의 기록입니다. 
프론트엔드 아키텍처 변경으로 인해 이 파일들은 삭제되었습니다.

## 삭제된 파일 목록

### HTML 템플릿 파일들

#### 메인 템플릿
- `src/main/resources/templates/index.html` (12KB, 210 lines) - 메인 인덱스 페이지
- `src/main/resources/templates/login.html` (23KB, 378 lines) - 로그인 페이지
- `src/main/resources/templates/chat.html` (30KB, 651 lines) - 채팅 페이지
- `src/main/resources/templates/game.html` (55KB, 1147 lines) - 게임 메인 페이지
- `src/main/resources/templates/game-play.html` (17KB, 289 lines) - 게임 플레이 페이지
- `src/main/resources/templates/game-select.html` (19KB, 404 lines) - 게임 선택 페이지
- `src/main/resources/templates/game-htmx.html` (8.3KB, 186 lines) - HTMX 기반 게임 페이지

#### 레이아웃
- `src/main/resources/templates/layouts/layout.html` (24KB, 469 lines) - 기본 레이아웃 템플릿

#### 프래그먼트
- `src/main/resources/templates/fragments/game-timer.html` (5.0KB, 101 lines) - 게임 타이머 컴포넌트
- `src/main/resources/templates/fragments/character-htmx.html` (11KB, 200 lines) - 캐릭터 HTMX 컴포넌트
- `src/main/resources/templates/fragments/status-content.html` (5.8KB, 105 lines) - 상태 표시 컴포넌트
- `src/main/resources/templates/fragments/character-modal-content.html` (11KB, 186 lines) - 캐릭터 모달 내용
- `src/main/resources/templates/fragments/header.html` (7.7KB, 113 lines) - 헤더 컴포넌트
- `src/main/resources/templates/fragments/character-modal.html` (5.0KB, 102 lines) - 캐릭터 모달
- `src/main/resources/templates/fragments/debug-auth.html` (4.4KB, 70 lines) - 디버그 인증 컴포넌트

### JavaScript 파일들

#### Thymeleaf 전용 JS
- `src/main/resources/static/js/thymeleaf/game/game-app.js` (16KB, 430 lines) - 게임 앱 메인 로직
- `src/main/resources/static/js/thymeleaf/game/game-play.js` (37KB, 917 lines) - 게임 플레이 로직

### CSS 파일들

#### Thymeleaf 전용 스타일
- `src/main/resources/static/css/timer-pressure.css` (5.2KB, 187 lines) - 타이머 압박감 표시 스타일
- `src/main/resources/static/css/dungeontalk-theme.css` (8.6KB, 332 lines) - DungeonTalk 테마 스타일

## 주요 기능들

### 게임 타이머 시스템
- 15분 TRPG 게임 타이머
- 단계별 압박감 표시 (여유 → 보통 → 긴급 → 위기)
- 시각적 프로그레스 바와 애니메이션
- 단계별 이모지와 색상 변화

### 캐릭터 관리
- 캐릭터 생성 및 수정 모달
- HTMX 기반 동적 업데이트
- 캐릭터 상태 표시

### 게임 시스템
- 게임 선택 및 플레이 인터페이스
- 실시간 상태 업데이트
- 반응형 디자인

### 인증 및 디버깅
- 로그인 시스템
- 디버그 인증 컴포넌트

## 삭제 이유
1. **프론트엔드 아키텍처 변경**: Thymeleaf에서 모던 프론트엔드 프레임워크로 전환
2. **유지보수성 향상**: 서버사이드 렌더링에서 클라이언트사이드 렌더링으로 변경
3. **성능 개선**: 정적 자산과 동적 렌더링의 분리
4. **개발 효율성**: 프론트엔드와 백엔드의 명확한 분리

## 마이그레이션 노트
- 기존 Thymeleaf 템플릿의 로직은 프론트엔드 컴포넌트로 재구현
- CSS 스타일은 프론트엔드 프로젝트로 이전
- JavaScript 로직은 모던 프레임워크에 맞게 리팩토링
- 데이터 바인딩은 REST API를 통한 JSON 통신으로 변경

---
*이 문서는 2025년 1월에 작성되었으며, Thymeleaf 관련 파일들이 삭제된 후의 참고 자료로 보관됩니다.*
