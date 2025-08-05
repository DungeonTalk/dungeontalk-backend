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

    // ======================= 불필요 API 패스 로직 =========================

    /**
     * 권한 불필요 API 들을 넘기는 메서드
     *
     * @param request 사용자 요청
     * @return ture/false
     */
    private boolean isPublicApi(HttpServletRequest request) {
        String path = request.getRequestURI();
        return PUBLIC_APIS.stream().anyMatch(path::startsWith);
    }

    private static final List<String> PUBLIC_APIS = List.of(
            "/v1/member/register",
            "/v1/member/login",
            "/swagger-ui",
            "/v3/api-docs"
           );

    // ======================= Filter Chain 로직 =========================

    /**
     * 필터의 핵심 메서드 (필터 체인)
     *
     * @param request     Http 요청 객체
     * @param response    Http 응답 객체
     * @param filterChain 필터 체인의 나머지 필터들을 호출할 때 사용
     *
     * @throws ServletException
     * @throws IOException
     */
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
            log.error("JWT 인증 중 오류 발생: {}", ex.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
        }
    }
}
