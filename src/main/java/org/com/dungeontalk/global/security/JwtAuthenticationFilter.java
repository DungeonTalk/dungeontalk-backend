package org.com.dungeontalk.global.security;


import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.member.entity.Member;
import org.com.dungeontalk.global.config.SecurityConfig;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final JwtExtractor jwtExtractor;
    private final SecurityConfig securityConfig;
    private final JwtProvider jwtProvider;

//    @Override
//    protected boolean shouldNotFilter(HttpServletRequest request) {
//        String path = request.getRequestURI();
//        return securityConfig.getPublicUrls().stream().anyMatch(path::startsWith);
//    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return securityConfig.getPublicUrls().stream()
                .anyMatch(p -> p.endsWith("/**")
                        ? path.startsWith(p.replace("/**",""))
                        : path.equals(p) || path.startsWith(p));
    }

    // ======================= DEPRECATED CODE - 3일간 관찰 한 후 문제 없으면 삭제 예정 =========================

//    private boolean isPublicApi(HttpServletRequest request) {
//        String path = request.getRequestURI();
//        List<String> publicApis = List.of(
//                "/v1/member/register",
//                "/v1/auth/login",
//                "/v1/valkey/session/keys",
//                "/v1/valkey/session/all",
//                "/v1/auth/refresh",
//                "/v1/valkey/session/test/save",
//                "/v1/stat/",
//                "/v1/characters",
//                "/init/",
//                "/stat-calculator.html",
//                "/character-test.html",
//                "/dungeon-game.html",
//                "/ws-chat",
//                // Swagger UI 관련 경로들
//                "/swagger-ui",
//                "/v3/api-docs",
//                "/webjars",
//                "/swagger-resources"
//        );
//
//        // 요청 경로가 publicApis 목록 중 하나로 시작하면 true 반환
//        return publicApis.stream().anyMatch(path::startsWith);
//    }

    // 필터 체인
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {

            // ======================= DEPRECATED CODE - 3일간 관찰 한 후 문제 없으면 삭제 예정 =========================

//            if (isPublicApi(request)) {
//                // 공개 API는 인증 없이 통과
//                filterChain.doFilter(request, response);
//                return;
//            }

            String accessToken = jwtExtractor.extractAccessToken(request);
            if (accessToken == null || accessToken.isEmpty()) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Access Token is missing");
                return;
            }

            // 토큰으로부터 CustomUserDetails 추출
            CustomUserDetails userDetails = jwtService.getUserDetailsFromToken(accessToken);

            JwtAuthenticationToken authentication = new JwtAuthenticationToken(userDetails);
            authentication.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            log.error("JWT 인증[필터] 중 오류 발생 : {}", ex.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
        }
    }



}
