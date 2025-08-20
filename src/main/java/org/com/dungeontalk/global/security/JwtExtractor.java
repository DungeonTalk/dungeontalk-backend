package org.com.dungeontalk.global.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class JwtExtractor {

    // 유저 요청으로 부터 엑세스 토큰 추출 (헤더 우선, 쿠키 대안)
    public String extractAccessToken(HttpServletRequest request) {
        // 1. Authorization 헤더에서 추출 시도
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        
        // 2. 쿠키에서 accessToken 추출 시도
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        return null;
    }
}
