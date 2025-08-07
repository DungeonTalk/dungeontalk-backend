package org.com.dungeontalk.global.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.member.entity.Member;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    // 권한 체크가 불필요한 API들을 패스하는 메서드
    private boolean isPublicApi(HttpServletRequest request) {

        return true;  // 모든 요청 인증 우회
//        String path = request.getRequestURI();
//        return PUBLIC_APIS.stream().anyMatch(path::startsWith);
    }

    // 권한 체크가 불필요한 API 리스트 정의 메서드
//    private static final List<String> PUBLIC_APIS = List.of(
//            "/v1/member/register",
//            "/v1/auth/login",
//            "/swagger-ui",
//            "/v3/api-docs",
//            "/v1/valkey/session/all",
//            "/v1/valkey/session/test/save"
//           );

    // 필터 체인
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            if (isPublicApi(request)) {
                // 공개 API는 인증 없이 통과
                filterChain.doFilter(request, response);
                return;
            }

            String accessToken = jwtService.extractAccessToken(request);

            if (accessToken == null || accessToken.isEmpty()) {
                // 토큰 없으면 401 Unauthorized
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Access Token is missing");
                return;
            }

            // 토큰 유효성 검사 및 멤버 조회
            Member member = jwtService.getMemberFromToken(accessToken);

            // 인증 정보 생성 및 SecurityContext에 저장
            JwtAuthenticationToken authentication = new JwtAuthenticationToken(member);
            authentication.setAuthenticated(true);
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);

            // 다음 필터로 이동
            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            log.error("JWT 인증[필터] 중 오류 발생 : {}", ex.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
        }
    }

}
