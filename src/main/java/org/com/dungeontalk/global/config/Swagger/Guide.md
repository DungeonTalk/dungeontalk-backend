# Dungeon Talk 백엔드 API - Swagger 설정

연결 링크 : http://localhost:8080/swagger-ui/index.html

## 개요
이 문서는 Spring Boot 애플리케이션에서 Springdoc OpenAPI를 사용한 Dungeon Talk 백엔드 API의 Swagger 설정을 설명합니다.


## 설정 세부 정보
- **클래스**: `SwaggerConfig`
    - `@Configuration` 어노테이션으로 Spring 설정 클래스임을 나타냄.
    - `@Bean` 메서드 `openAPI()`를 정의하여 OpenAPI 명세를 설정.
- **OpenAPI 설정**:
    - **제목**: Dungeon Talk Backend API
    - **버전**: v0.0.1
    - **설명**: F6 팀 API 명세서
    - OpenAPI 설정을 위한 `Components` 객체 포함.
- **접근 경로**: 애플리케이션이 로컬에서 실행 중일 때 `http://localhost:8080/swagger-ui/index.html`에서 Swagger UI에 접근 가능.

## 사용 방법
- Spring Boot 애플리케이션을 실행.
- `http://localhost:8080/swagger-ui/index.html`로 이동하여 API 문서 확인.
- Swagger UI를 통해 사용 가능한 API 엔드포인트를 탐색하고 테스트.
