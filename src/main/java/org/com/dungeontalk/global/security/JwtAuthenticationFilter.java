package org.com.dungeontalk.global.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.member.entity.Member;
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
                
                // 토큰 유효성 검사 및 멤버 조회
                Member member = jwtService.getMemberFromToken(accessToken);
                
                if (member != null) {
                    log.debug("Member extracted from token: {}", member.getName());
                    
                    // 인증 정보 생성 및 SecurityContext에 저장
                    CustomUserDetails userDetails = new CustomUserDetails(member);
                    JwtAuthenticationToken authentication = new JwtAuthenticationToken(userDetails);
                    authentication.setAuthenticated(true);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("Authentication set in SecurityContext: {}", authentication.getPrincipal());
                }
            }
            
            // 다음 필터로 이동 (Spring Security가 권한 체크 처리)
            filterChain.doFilter(request, response);
            
        } catch (Exception ex) {
            log.error("JWT 인증[필터] 중 오류 발생 : {}", ex.getMessage());
            // 에러가 발생해도 필터 체인 계속 진행 (Spring Security가 처리)
            filterChain.doFilter(request, response);
        }
    }



}
