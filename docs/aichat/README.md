# AI 채팅 시스템 문서

DungeonTalk의 AI 채팅 시스템에 대한 종합적인 문서입니다.

## 📚 문서 목록

- [🏗️ 시스템 아키텍처](./architecture.md) - AI 채팅 시스템의 전체 구조와 컴포넌트
- [🚀 API 가이드](./api-guide.md) - REST API 및 WebSocket API 사용법
- [⚡ 실시간 통신 가이드](./websocket-guide.md) - WebSocket STOMP 메시징 시스템
- [🤖 AI 서비스 연동 가이드](./ai-service-integration.md) - Python AI 서비스 연동 방법
- [🔧 시스템 설정 가이드](./configuration.md) - 시스템 설정 및 환경 구성
- [🧪 테스트 가이드](./testing-guide.md) - 테스트 도구 및 방법론
- [🎮 게임 로직 문서](./game-logic.md) - 자유형 게임 시스템 및 상태 관리
- [🎯 게임 페이즈 참조서](./game-phases.md) - 게임 페이즈별 상세 설명 및 전환 규칙
- [🚨 문제 해결 가이드](./troubleshooting.md) - 일반적인 오류 해결 및 디버깅

## 🎯 주요 기능

### 1. 실시간 AI 채팅
- WebSocket STOMP 기반 실시간 양방향 통신
- 다중 사용자 게임방 지원
- AI 응답 생성 및 브로드캐스팅

### 2. 게임방 관리
- 게임방 생성, 조회, 시작, 종료
- 참여자 관리 및 권한 제어
- 게임 상태 및 페이즈 관리

### 3. AI 서비스 연동
- Python AI 서비스와 RESTful API 통신
- 대화 컨텍스트 관리
- 비동기 응답 처리

### 4. 동시성 제어
- Redis 분산 락을 통한 중복 요청 방지
- 게임 상태 동기화
- 실시간 메시지 순서 보장

## 🔗 관련 링크

- [Backend Repository](https://github.com/your-repo/dungeontalk-backend)
- [API 가이드](./api-guide.md)
- [게임 로직 문서](./game-logic.md)
- [문제 해결 가이드](./troubleshooting.md)

## 📝 업데이트 로그

- **2025-08-08**: 초기 AI 채팅 시스템 구현 완료
- **2025-08-08**: WebSocket STOMP 기반 실시간 통신 추가
- **2025-08-08**: Python AI 서비스 연동 구현
- **2025-08-08**: 게임 로직 및 상태 관리 시스템 구현
- **2025-08-08**: 종합 문서화 완료 (8개 가이드 문서)