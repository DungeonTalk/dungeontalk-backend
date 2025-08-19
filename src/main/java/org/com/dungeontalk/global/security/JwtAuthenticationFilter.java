package org.com.dungeontalk.global.security;


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

            // System.out.println("공개 API 통과");
            String accessToken = jwtExtractor.extractAccessToken(request);

            System.out.println("엑세스 토큰" + accessToken);
            if (accessToken == null || accessToken.isEmpty()) {
                // 토큰 없으면 401 Unauthorized
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Access Token is missing");
                return;
            }

            System.out.println("엑세스 토큰 검증 완료");
            // 토큰 유효성 검사 및 멤버 조회
            Member member = jwtService.getMemberFromToken(accessToken);
            System.out.println("토큰으로 부터 멤버 추출" + member);

            // 인증 정보 생성 및 SecurityContext에 저장
            CustomUserDetails userDetails = new CustomUserDetails(member);
            JwtAuthenticationToken authentication = new JwtAuthenticationToken(userDetails);
            authentication.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            System.out.println("authentication : "+ authentication);

            // 다음 필터로 이동
            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            log.error("JWT 인증[필터] 중 오류 발생 : {}", ex.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
        }
    }



}
