package org.com.dungeontalk.global.websocket;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.global.security.JwtService;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor extends HttpSessionHandshakeInterceptor {

    private final JwtService jwtService;

    public boolean beforeHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Map<String, Object> attributes) throws Exception {

        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();

            String token = httpRequest.getParameter("token");
            log.info("🔥 WebSocket Handshake token: {}", token);

            if (token == null) {
                log.warn("❌ WebSocket 인증 실패: 토큰 없음");
                return false;
            }

            if (!jwtService.validateToken(token)) {
                log.warn("❌ WebSocket 인증 실패: 유효하지 않은 토큰");
                return false;
            }

            if (!jwtService.isTokenBlacklisted(token)) {
                log.warn("❌ WebSocket 인증 실패: Redis에 저장되지 않은 토큰 (로그아웃 상태)");
                return false;
            }

            log.info("✅ WebSocket 인증 성공");

            return super.beforeHandshake(request, response, wsHandler, attributes);
        }

        log.warn("❌ WebSocket 인증 실패: 요청 객체가 HttpServletRequest 아님");
        return false;
    }

}
