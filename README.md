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

## 🧭 ERD

```mermaid
erDiagram
  %% =========================
  %% RDB (PostgreSQL)
  %% =========================
  MEMBER {
    string id PK
    datetime created_at
    datetime updated_at
    string name
    string nick_name
    string password
  }

  AUTH {
    string id PK
    datetime created_at
    datetime updated_at
    string access_token
    string token_type
    string refresh_token
    string email
    string member_id FK
  }

  %% 캐릭터(신규 스키마)
  CHARACTER {
    string id PK
    datetime created_at
    datetime updated_at
    string member_id FK
    int player_level
    int total_exp
    int unspent_points
    int str
    int dex
    int int
    int wil
    int wis
    int luk
    int race_type_id FK
  }

  %% 캐릭터(기존/대안 스키마)
  GAME_CHARACTER {
    string id PK
    datetime created_at
    datetime updated_at
    string member_id FK
    int player_level
    int total_exp
    int unspent_points
    int str
    int dex
    int int
    int wil
    int wis
    int luk
    int strength
    int dexterity
    int intelligence
    int willpower
    int wisdom
    int luck
    int race_id FK
  }

  RACE_STATS {
    string id PK
    datetime created_at
    datetime updated_at
    string race
    int hp
    int mp
    int health_points
    int mana_points
    int accuracy
    int dice_odds
    int evasion_rate
    int physical_attack
    int magic_attack
  }

  WORLD_TYPES {
    int id PK
    string code
    string display_name
    string description
    string game_settings
    boolean is_active
    int sort_order
    datetime created_at
    datetime updated_at
  }

  WORLD {
    int world_id PK
    string world_name
    int clear_exp
  }

  REQUEST_EXP {
    int level PK
    int request_next_level_exp
    int request_total_exp
  }

  %% RDB 관계
  MEMBER ||--o{ AUTH           : has
  MEMBER ||--o{ CHARACTER      : owns
  MEMBER ||--o{ GAME_CHARACTER : owns
  GAME_CHARACTER }o--|| RACE_STATS  : race_id_ref
  CHARACTER     }o--|| WORLD_TYPES : race_type_ref

  %% (선택) 캐릭터가 특정 월드 진행도를 가진다면 연결 가능
  %% CHARACTER }o--o{ WORLD : progress_in

  %% =========================
  %% MongoDB (채팅/AI 게임)
  %% =========================
  CHAT_ROOM {
    string id PK
    string name
    string room_type
    int max_participants
    string[] participants
    datetime created_at
    datetime updated_at
  }

  CHAT_MESSAGE {
    string id PK
    string room_id FK
    string sender_id
    string receiver_Id
    string content
    string message_type
    datetime created_at
    datetime updated_at
  }
 
  CHAT_ROOM_MEMBER {
     string id PK
     string room_id FK
     string member_id
     string status
     datetime joinedAt
     datetime updateAt
     datetime leftAt
  }

  AI_GAME_ROOM {
    string id PK
    string base_room_id
    string world_type
    string prompt
    string state
    string[] participants
    datetime created_at
    datetime updated_at
  }

  AI_GAME_MESSAGE {
    string id PK
    string ai_game_room_id FK
    string content
    string message_type
    int turn_number
    int message_order
    datetime created_at
  }

  CHAT_ROOM    ||--o{ CHAT_MESSAGE    : contains
  AI_GAME_ROOM ||--o{ AI_GAME_MESSAGE : contains

  %% =========================
  %% Cross-DB (논리 연결)
  %% =========================
  MEMBER ||--o{ CHAT_ROOM     : participants_ref
  MEMBER ||--o{ CHAT_MESSAGE  : sender_ref
  MEMBER ||--o{ AI_GAME_ROOM  : participants_ref
```


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

<br/>

### 아키텍쳐 구성도
<img width="900" height="750" alt="image" src="https://github.com/user-attachments/assets/55b1f124-0a41-4206-9c6d-4393074ff1f0" />


### CI/CD 구성도
<img width="900" height="750" alt="image" src="https://github.com/user-attachments/assets/d0f18382-b2f4-4103-a5bb-23e6f6cf2784" />

<br/>

## 🛠️ 기술 스택

| 구분               | 기술/도구                                                             |
| ---------------- | ----------------------------------------------------------------- |
| **Frontend** | HTML, CSS, JavaScript, Thymeleaf, SockJS |
| **Backend** | Java 21 (LTS), Spring Boot 3.4.0, Spring Security, JWT            |
| **DataBase**       | PostgreSQL 17 (pgvector 지원), MongoDB 7.0, Redis(Valkey) - (캐시/세션) |
| **Main Tech Stack**       | WebSocket, STOMP, Redis (Valkey) Pub/Sub, RAG                                   |
| **Dev Tools**        | Swagger/OpenAPI (API 문서화), JUnit 5, Mockito, Docker (컨테이너화), Discord          |

<br/>

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


## 🗂 더 알아보고 싶은 내용이 있다면?
더 궁금한 내용이 있으시면 아래를 참고해주세요.
- 협업 방식을 알고 싶다. 👉 [협업 가이드 바로가기](https://github.com/codesche)
- 도메인별 API 상세정보가 궁금하다. 👉 [DungeonTalk API 문서 바로가기](https://github.com/DungeonTalk/dungeontalk-backend/wiki/DungeonTalk-API-%EB%AC%B8%EC%84%9C)
- 사용한 기술에 대한 도입 배경이 궁금하다. 👉 [기술 스택 바로가기](https://github.com/DungeonTalk/dungeontalk-backend/wiki/%F0%9F%94%A8-%EA%B0%9C%EB%B0%9C-%ED%99%98%EA%B2%BD-%EA%B5%AC%EC%84%B1-&-%EA%B8%B0%EC%88%A0-%EC%8A%A4%ED%83%9D)
- 주요 기능 개발 과정이 궁금하다. 👉 [기술 상세 정보 바로가기](https://github.com/codesche)
- CI/CD 구성 절차가 궁금하다. 👉 [CI-CD 구성 바로가기](https://github.com/codesche)
- ERD와 DB 정보에 대해 궁금하다. 👉 [ERD 정보 바로가기](https://github.com/DungeonTalk/dungeontalk-backend/wiki/%F0%9F%93%9A-ERD-%EC%A0%95%EB%B3%B4)
- 트러블 슈팅에 대한 내용이 궁금하다. 👉 [트러블 슈팅 바로가기](https://github.com/codesche)
- 프로젝트 진행 과정에 대한 내용이 궁금하다. 👉 [DungeonTalk 기술 블로그](https://github.com/codesche)
- 도메인별 테스트 코드 내용이 궁금하다. 👉 [DungeonTalk 도메인별 테스트 코드 작성](https://github.com/codesche)

기타 다른 내용들은 [📝 팀 위키](https://github.com/DungeonTalk/dungeontalk-backend/wiki) 에서 확인할 수 있습니다.

<!--
더 궁금한 내용이 있으시면 아래를 참고해주세요.
- 협업 방식을 알고 싶다. 👉 [협업 가이드 바로가기](https://github.com/DoDreamTeam/Backend/wiki/%F0%9F%93%9C-%ED%98%91%EC%97%85-%EA%B0%80%EC%9D%B4%EB%93%9C)
- 프로젝트 수행 계획이 궁금하다. 👉 [프로젝트 수행 계획 바로가기](https://github.com/DoDreamTeam/Backend/wiki/DoDream-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8-%EC%88%98%ED%96%89%EA%B3%84%ED%9A%8D)
- 사용한 기술이 궁금하다. 👉 [기술 스택 바로가기](https://github.com/DoDreamTeam/Backend/wiki/%F0%9F%97%82-%EA%B8%B0%EC%88%A0-%EC%8A%A4%ED%83%9D)
- ERD와 DB 정보에 대해 궁금하다. 👉 [ERD 정보 바로가기](https://github.com/DoDreamTeam/Backend/wiki/ERD-%EC%A0%95%EB%B3%B4)
- 주요 기능이 궁금하다. 👉 [DoDream 사용 설명서 바로가기](https://github.com/DoDreamTeam/Backend/wiki/%EC%82%AC%EC%9A%A9-%EC%84%A4%EB%AA%85%EC%84%9C)
- 주요 기능과 관련된 코드가 궁금하다. 👉 [기술 상세 정보 바로가기](https://github.com/DoDreamTeam/Backend/wiki/DoDream-%EA%B8%B0%EC%88%A0-%EB%AA%85%EC%84%B8%EC%84%9C)
- 핵심 기술과 기술적 시도에 대한 내용이 궁금하다. 👉 [핵심 기술 + 기술적 시도 바로가기](https://github.com/DoDreamTeam/Backend/wiki/%ED%95%B5%EC%8B%AC-%EA%B8%B0%EC%88%A0-&-%EA%B8%B0%EC%88%A0%EC%A0%81-%EC%8B%9C%EB%8F%84)
- CI/CD 절차가 궁금하다. 👉 [CI-CD 구성 바로가기](https://github.com/DoDreamTeam/Backend/wiki/Docker%EC%99%80-Github-Action%EC%9D%84-%ED%99%9C%EC%9A%A9%ED%95%9C-CI-CD-%EA%B5%AC%EC%84%B1)
- 트러블 슈팅에 대한 내용이 궁금하다. 👉 [트러블 슈팅 바로가기](https://github.com/DoDreamTeam/Backend/wiki/DoDream-%ED%8A%B8%EB%9F%AC%EB%B8%94-%EC%8A%88%ED%8C%85)
- 프로젝트를 하면서 경험한 내용이 궁금하다. 👉 [기술 블로그 바로가기](https://github.com/DoDreamTeam/Backend/wiki/%F0%9F%92%BB-%EA%B0%9C%EB%B0%9C-%EB%B8%94%EB%A1%9C%EA%B7%B8)
- 프로젝트에 대한 회고록이 궁금하다. 👉 [DoDream 프로젝트 회고록 바로가기](https://github.com/DoDreamTeam/Backend/wiki/DoDream-%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8-%ED%9A%8C%EA%B3%A0%EB%A1%9D)

기타 다른 내용들은 [📝 팀 위키](https://github.com/DoDreamTeam/Backend/wiki) 에서 확인할 수 있습니다.
-->
