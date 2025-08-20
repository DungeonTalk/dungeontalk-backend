package org.com.dungeontalk.domain.auth.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class CookieManagerTest {

    @Test
    @DisplayName("HttpOnly/secure/path/maxAge 설정")
    void addRefreshTokenCookie() {
        CookieManager mgr = new CookieManager();
        ReflectionTestUtils.setField(mgr, "REFRESH_TOKEN_EXPIRATION_TIME", 3600L);

        HttpServletResponse res = mock(HttpServletResponse.class);
        ArgumentCaptor<Cookie> cap = ArgumentCaptor.forClass(Cookie.class);

        mgr.addRefreshTokenCookie(res, "rt");

        verify(res).addCookie(cap.capture());
        Cookie c = cap.getValue();

        assertThat(c.getName()).isEqualTo("refreshToken");
        assertThat(c.getValue()).isEqualTo("rt");
        assertThat(c.isHttpOnly()).isTrue();
        assertThat(c.getSecure()).isTrue();
        assertThat(c.getPath()).isEqualTo("/");
        assertThat(c.getMaxAge()).isEqualTo(3600);
    }

    @Test
    @DisplayName("Max-Age 0으로 즉시 삭제")
    void clearRefreshTokenCookie() {
        CookieManager mgr = new CookieManager();
        HttpServletResponse res = mock(HttpServletResponse.class);
        ArgumentCaptor<Cookie> cap = ArgumentCaptor.forClass(Cookie.class);

        mgr.clearRefreshTokenCookie(res);

        verify(res).addCookie(cap.capture());
        Cookie cookie = cap.getValue();

        assertThat(cookie.getName()).isEqualTo("refreshToken");
        assertThat(cookie.getValue()).isNull();
        assertThat(cookie.getMaxAge()).isZero();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
    }

}