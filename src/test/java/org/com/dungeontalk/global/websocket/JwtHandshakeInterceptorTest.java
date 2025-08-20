package org.com.dungeontalk.global.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.com.dungeontalk.global.security.JwtProvider;
import org.com.dungeontalk.global.security.JwtRedisService;
import org.com.dungeontalk.global.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.WebSocketHandler;

@ExtendWith(MockitoExtension.class)
class JwtHandshakeInterceptorTest {

    @Mock
    JwtService jwtService;

    @Mock
    JwtProvider jwtProvider;

    @Mock
    JwtRedisService jwtRedisService;

    @Mock
    WebSocketHandler webSocketHandler;

    @InjectMocks
    JwtHandshakeInterceptor interceptor; // 대상

    private ServletServerHttpRequest req(MockHttpServletRequest mockHttpServletRequest) {
        return new ServletServerHttpRequest(mockHttpServletRequest);
    }
    private ServletServerHttpResponse res(MockHttpServletResponse mockHttpServletResponse) {
        return new ServletServerHttpResponse(mockHttpServletResponse);
    }

    @Test
    @DisplayName("query token + roomId → 검증 통과 → attributes 저장 & true")
    void ok_withQueryToken() throws Exception {
        MockHttpServletRequest http = new MockHttpServletRequest();
        http.setParameter("token", "tok-1234-5678");
        http.setParameter("roomId", "room-1");

        when(jwtProvider.validateToken("tok-1234-5678")).thenReturn(true);
        when(jwtRedisService.isTokenBlacklisted("tok-1234-5678")).thenReturn(false);
        when(jwtService.extractIdFromToken("tok-1234-5678")).thenReturn("M-001");

        Map<String,Object> attrs = new HashMap<>();
        boolean ok = interceptor.beforeHandshake(
            req(http), res(new MockHttpServletResponse()), webSocketHandler, attrs);

        assertThat(ok).isTrue();
        assertThat(attrs.get("memberId")).isEqualTo("M-001");
        assertThat(attrs.get("roomId")).isEqualTo("room-1");
    }

    @Test
    @DisplayName("Authorization: Bearer 헤더로 토큰 전달도 허용")
    void ok_withAuthHeader() throws Exception {
        MockHttpServletRequest http = new MockHttpServletRequest();
        http.addHeader("Authorization", "Bearer abc.def");
        http.setParameter("roomId", "room-9");

        when(jwtProvider.validateToken("abc.def")).thenReturn(true);
        when(jwtRedisService.isTokenBlacklisted("abc.def")).thenReturn(false);
        when(jwtService.extractIdFromToken("abc.def")).thenReturn("M-777");

        Map<String,Object> attrs = new HashMap<>();
        boolean ok = interceptor.beforeHandshake(
            req(http), res(new MockHttpServletResponse()), webSocketHandler, attrs);

        assertThat(ok).isTrue();
        assertThat(attrs.get("memberId")).isEqualTo("M-777");
        assertThat(attrs.get("roomId")).isEqualTo("room-9");
    }

    @Test
    @DisplayName("token/roomId 누락됨 → 400 & false")
    void badRequest_missingParams() throws Exception {
        MockHttpServletRequest http = new MockHttpServletRequest();
        // token, roomId 없음
        MockHttpServletResponse response = new MockHttpServletResponse();

        Map<String,Object> attrs = new HashMap<>();
        boolean ok = interceptor.beforeHandshake(
            req(http), res(response), webSocketHandler, attrs);

        assertThat(ok).isFalse();
        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("토큰 검증 실패 → 401 & false")
    void invalidToken() throws Exception {
        MockHttpServletRequest http = new MockHttpServletRequest();
        http.setParameter("token", "bad");
        http.setParameter("roomId", "room-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.validateToken("bad")).thenReturn(false);

        Map<String,Object> attrs = new HashMap<>();
        boolean ok = interceptor.beforeHandshake(
            req(http), res(response), webSocketHandler, attrs);

        assertThat(ok).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("블랙리스트 토큰 → 401 & false")
    void blacklistedToken() throws Exception {
        MockHttpServletRequest http = new MockHttpServletRequest();
        http.setParameter("token", "blk");
        http.setParameter("roomId", "room-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.validateToken("blk")).thenReturn(true);
        when(jwtRedisService.isTokenBlacklisted("blk")).thenReturn(true);

        Map<String,Object> attrs = new HashMap<>();
        boolean ok = interceptor.beforeHandshake(
            req(http), res(response), webSocketHandler, attrs);

        assertThat(ok).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("memberId 추출 실패 → false (상태코드는 지정 안 함)")
    void extractIdFail() throws Exception {
        MockHttpServletRequest http = new MockHttpServletRequest();
        http.setParameter("token", "tok");
        http.setParameter("roomId", "room-1");

        when(jwtProvider.validateToken("tok")).thenReturn(true);
        when(jwtRedisService.isTokenBlacklisted("tok")).thenReturn(false);
        when(jwtService.extractIdFromToken("tok")).thenThrow(new RuntimeException("boom"));

        Map<String,Object> attrs = new HashMap<>();
        boolean ok = interceptor.beforeHandshake(
            req(http), res(new MockHttpServletResponse()), webSocketHandler, attrs);

        assertThat(ok).isFalse();
        assertThat(attrs).doesNotContainKeys("memberId", "roomId");
    }
}