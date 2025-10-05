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



    // 필터 체인
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        log.debug("JWT Filter - Request URI: {}", requestURI);
        
        try {
            String accessToken = jwtExtractor.extractAccessToken(request);
            
            // 토큰이 있으면 인증 처리
            if (accessToken != null && !accessToken.isEmpty()) {
                log.debug("Access token found, validating...");
                
                // 토큰으로부터 CustomUserDetails 추출
                CustomUserDetails userDetails = jwtService.getUserDetailsFromToken(accessToken);
                
                if (userDetails != null) {
                    log.debug("User details extracted from token: {}", userDetails.getUsername());
                    
                    // 인증 정보 생성 및 SecurityContext에 저장
                    JwtAuthenticationToken authentication = new JwtAuthenticationToken(userDetails);
                    authentication.setAuthenticated(true);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("Authentication set in SecurityContext: {}", authentication.getPrincipal());
                }
            }
            
        } catch (Exception ex) {
            log.error("JWT 인증[필터] 중 오류 발생 : {}", ex.getMessage());
        }
        
        // 다음 필터로 이동 (Spring Security가 권한 체크 처리)
        filterChain.doFilter(request, response);
    }



}
