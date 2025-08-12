package org.com.dungeontalk.domain.auth.manager;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieManager {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    @Value("${jwt.refreshexpiration}")
    private long REFRESH_TOKEN_EXPIRATION_TIME;

    /**
     * HttpOnly 쿠키에 Refresh Token 저장
     */
    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // HTTPS에서만 전송
        cookie.setPath("/");
        cookie.setMaxAge((int) REFRESH_TOKEN_EXPIRATION_TIME);
        response.addCookie(cookie);
    }

    /**
     * Refresh Token 쿠키 삭제
     */
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // 즉시 삭제
        response.addCookie(cookie);
    }

}
