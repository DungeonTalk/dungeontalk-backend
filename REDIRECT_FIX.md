# 로그인 후 리다이렉트 문제 해결

## 문제 원인
1. Spring Security가 인증되지 않은 요청에 대해 기본적으로 `/login`으로 302 리다이렉트
2. JWT 기반 인증을 사용하는데 Spring Security의 세션 기반 인증 로직이 간섭

## 해결 방법
1. **SecurityConfig 수정**:
   - `exceptionHandling`을 추가하여 인증 실패 시 401 반환 (리다이렉트 방지)
   - `WebSecurityCustomizer`에 모든 Thymeleaf 뷰 경로 추가
   - JWT 필터의 PUBLIC_APIS 리스트 업데이트

2. **ViewController 수정**:
   - `@AuthenticationPrincipal` 의존성 제거
   - JWT 기반 인증이므로 클라이언트 사이드에서 토큰 체크

## 테스트 방법
1. 애플리케이션 재시작
2. http://localhost:8080/debug-login.html 에서 단계별 테스트
3. http://localhost:8080/login 에서 로그인 후 자동 리다이렉트 확인

## 추가 확인 사항
- 브라우저 개발자 도구에서 네트워크 탭 확인
- 302 리다이렉트가 발생하는지 확인
- 콘솔 로그에서 JavaScript 오류 확인