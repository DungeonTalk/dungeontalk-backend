package org.com.dungeontalk.domain.auth.manager;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieManager {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";

    @Value("${jwt.refreshexpiration}")
    private long REFRESH_TOKEN_EXPIRATION_TIME;
    
    @Value("${jwt.accessexpiration}")
    private long ACCESS_TOKEN_EXPIRATION_TIME;
    
    @Value("${cookie.secure:false}")
    private boolean secureCookie;

    /**
     * HttpOnly 쿠키에 Refresh Token 저장
     */
    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        addCookieWithSameSite(response, REFRESH_TOKEN_COOKIE_NAME, refreshToken, 
                              (int) REFRESH_TOKEN_EXPIRATION_TIME, true);
    }

    /**
     * Refresh Token 쿠키 삭제
     */
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        clearCookieWithSameSite(response, REFRESH_TOKEN_COOKIE_NAME, true);
    }

    /**
     * 쿠키에 Access Token 저장 (페이지 이동 시 인증을 위해)
     */
    public void addAccessTokenCookie(HttpServletResponse response, String accessToken) {
        addCookieWithSameSite(response, ACCESS_TOKEN_COOKIE_NAME, accessToken, 
                              (int) (ACCESS_TOKEN_EXPIRATION_TIME / 1000), true);
    }

    /**
     * Access Token 쿠키 삭제
     */
    public void clearAccessTokenCookie(HttpServletResponse response) {
        clearCookieWithSameSite(response, ACCESS_TOKEN_COOKIE_NAME, true);
    }
    
    /**
     * 모든 인증 관련 쿠키 일괄 삭제
     */
    public void clearAllAuthCookies(HttpServletResponse response) {
        // Refresh Token 쿠키 삭제
        clearRefreshTokenCookie(response);
        
        // Access Token 쿠키 삭제
        clearAccessTokenCookie(response);
        
        // JSESSIONID 쿠키도 삭제 (세션 완전 제거)
        clearCookieWithSameSite(response, "JSESSIONID", true);
    }
    
    /**
     * SameSite 속성을 포함한 쿠키 추가 (공통 메서드)
     */
    private void addCookieWithSameSite(HttpServletResponse response, String name, String value, 
                                       int maxAge, boolean httpOnly) {
        // Set-Cookie 헤더로 직접 설정 (SameSite 속성 포함)
        String cookieHeader = String.format("%s=%s; Path=/; Max-Age=%d%s; SameSite=Lax%s",
                name, value, maxAge,
                httpOnly ? "; HttpOnly" : "",
                secureCookie ? "; Secure" : "");
        response.addHeader("Set-Cookie", cookieHeader);
    }
    
    /**
     * SameSite 속성을 포함한 쿠키 삭제 (공통 메서드)
     */
    private void clearCookieWithSameSite(HttpServletResponse response, String name, boolean httpOnly) {
        String cookieHeader = String.format("%s=; Path=/; Max-Age=0%s; SameSite=Lax%s",
                name,
                httpOnly ? "; HttpOnly" : "",
                secureCookie ? "; Secure" : "");
        response.addHeader("Set-Cookie", cookieHeader);
    }

}
