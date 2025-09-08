# 🎲 DungeonTalk

## 📖 프로젝트 소개

- DungeonTalk는 AI와 함께하는 혁신적인 TRPG(테이블탑 롤플레잉 게임) 플랫폼입니다.
- 실시간 채팅을 통해 AI 게임마스터와 함께 특정 세계관에서의 모험을 즐길 수 있습니다.
- 플레이어는 직접 게임 속의 캐릭터가 되어 직접 스토리를 만들어 나가면서 다양한 상황을 체험할 수 있습니다.

<br/>

## 🎯 주요 특징

- 🤖 **AI 게임마스터**: 역동적이고 상호작용적인 TRPG Dungeontalk 게임의 진행을 담당하는 게임마스터
- ⚡ **실시간 통신**: WebSocket STOMP 기반 실시간 멀티플레이어 지원
- 🎮 **자유형 게임플레이**: 제약 없는 창의적인 롤플레잉 경험
- 🌍 **다양한 세계관**: 판타지, 좀비 아포칼립스 등의 테마로 구성
- 👥 **매칭 시스템**: 세계관별 자동 매칭 및 대기열 관리

<br/>

## 📸 프로젝트 이미지

### DungeonTalk 로그인 화면
<div align="center">

<img width="1000" height="700" alt="image" src="https://github.com/user-attachments/assets/b717d001-94ad-4825-a55d-f42098a766f7" />

</div>

<br/>

### DungeonTalk 메인 화면
<div align="center">

<img width="1000" height="700" alt="image" src="https://github.com/user-attachments/assets/d8ec8baf-30b0-44ef-ab05-f44578f3ed6d" />

</div>

<br/>

### AI와의 게임 진행 화면

<div align="center">

<img width="1000" height="700" alt="image" src="https://github.com/user-attachments/assets/f59fb092-6f33-452a-b6c9-da1e302116e8" />

</div>

<br/>

### 캐릭터 생성 및 관리 화면

<div align="center">

<img width="1000" height="700" alt="image" src="https://github.com/user-attachments/assets/404390e7-dd57-4170-b22a-4da094481dff" />

<img width="1000" height="700" alt="image" src="https://github.com/user-attachments/assets/bbca9ea7-112c-4b32-9449-acab8ba4ab66" />

<img width="1436" height="787" alt="image" src="https://github.com/user-attachments/assets/750fcdd4-582a-4d34-872c-5554b745bac2" />

</div>

<br/>

### 세계관별 매칭 화면

<div align="center">

<img width="1000" height="700" alt="image" src="https://github.com/user-attachments/assets/258777ca-35c2-4be6-be82-7612d121350e" />

</div>

<br/>

## 🏗️ 시스템 아키텍처

### 전반적인 흐름 구성도

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Backend        │    │   AI Service    │
│   (Web/Mobile)  │◄──►│   Spring Boot    │◄──►│   Python/RAG    │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │
                ┌───────────────┼───────────────┐
                │               │               │
        ┌───────▼──────┐ ┌──────▼──────┐ ┌─────▼──────┐
        │  PostgreSQL  │ │   Valkey    │ │  MongoDB   │
        │   (Main DB)  │ │ (Cache/Session)│ │(Vector DB) │
        └──────────────┘ └─────────────┘ └────────────┘
```

### CI/CD 구성도
<img width="900" height="750" alt="image" src="https://github.com/user-attachments/assets/d0f18382-b2f4-4103-a5bb-23e6f6cf2784" />



## 🛠️ 기술 스택

### Backend Core
- **Java 21** - 최신 LTS 버전
- **Spring Boot 3.4.0** - 웹 프레임워크
- **Spring Security** - 인증/인가
- **JWT** - 토큰 기반 인증

### 데이터베이스
- **PostgreSQL 17** - 메인 데이터베이스 (pgvector 지원)
- **MongoDB 7.0** - 벡터 데이터베이스 (AI 임베딩)
- **Valkey** - 캐시 및 세션 스토어

### 실시간 통신
- **WebSocket** - 실시간 양방향 통신
- **STOMP** - 메시징 프로토콜
- **Redis Pub/Sub** - 분산 메시징

### 개발 도구
- **Swagger/OpenAPI** - API 문서화
- **JUnit 5** - 테스트 프레임워크
- **Docker** - 컨테이너화

## 📁 프로젝트 구조

```
src/
├── main/
│   ├── java/org/com/dungeontalk/
│   │   ├── domain/                 # 도메인별 패키지
│   │   │   ├── aichat/            # AI 채팅 시스템
│   │   │   ├── auth/              # 인증/인가
│   │   │   ├── chat/              # 일반 채팅
│   │   │   ├── matching/          # 매칭 시스템
│   │   │   ├── gamecharacter/     # 게임 캐릭터
│   │   │   └── ...
│   │   ├── global/                # 전역 설정
│   │   │   ├── config/           # 설정 클래스
│   │   │   ├── security/         # 보안 설정
│   │   │   ├── websocket/        # WebSocket 설정
│   │   │   └── exception/        # 예외 처리
│   │   └── web/                  # 웹 컨트롤러
│   └── resources/
│       ├── application*.properties  # 환경별 설정
│       ├── static/                 # 정적 파일
│       └── templates/              # 템플릿 파일
└── test/                          # 테스트 코드
```

## 🎮 핵심 기능

### 1. AI 채팅 시스템 (`aichat`)
- **실시간 AI 대화**: WebSocket 기반 즉시 응답
- **컨텍스트 관리**: 대화 히스토리 및 게임 상태 유지
- **멀티플레이어**: 최대 4명까지 동시 플레이
- **게임 페이즈**: 구조화된 게임 진행 단계

### 2. 매칭 시스템 (`matching`)
- **세계관별 매칭**: 판타지, SF, 좀비 등 테마별 대기열
- **Redis 큐**: 고성능 매칭 알고리즘
- **실시간 상태**: 매칭 진행 상황 실시간 업데이트

### 3. 인증/인가 (`auth`)
- **JWT 토큰**: stateless 인증 방식
- **Refresh Token**: 보안 강화 토큰 갱신
- **무차별 대입 방지**: Redis 기반 로그인 시도 제한

### 4. 캐릭터 시스템 (`gamecharacter`)
- **스탯 시스템**: D&D 스타일 능력치 관리
- **레벨 시스템**: 경험치 기반 성장
- **종족별 특성**: 다양한 판타지 종족 지원

## 📚 상세 문서

- [🏗️ 시스템 아키텍처](docs/aichat/architecture.md)
- [🚀 API 가이드](docs/aichat/api-guide.md)
- [⚡ WebSocket 가이드](docs/aichat/websocket-guide.md)
- [🤖 AI 서비스 연동](docs/aichat/ai-service-integration.md)
- [🎮 게임 로직](docs/aichat/game-logic.md)
- [🧪 테스트 가이드](docs/aichat/testing-guide.md)
- [🚨 트러블슈팅](docs/aichat/troubleshooting.md)

---

<div align="center">

**🎲 DungeonTalk와 함께 새로운 모험을 시작하세요! 🎲**

[🌟 프로젝트 홈](https://github.com/your-repo/dungeontalk) · [📖 문서](docs/) · [🐛 이슈 신고](https://github.com/your-repo/dungeontalk/issues)

</div>
