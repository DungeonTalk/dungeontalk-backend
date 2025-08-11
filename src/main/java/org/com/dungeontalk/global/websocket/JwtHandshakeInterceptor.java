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

            // 1) token, roomId를 쿼리에서 받기
            String token = httpRequest.getParameter("token");
            String roomId = httpRequest.getParameter("roomId");

            log.info("🔥 WebSocket Handshake token: {}", token);

            // Authorization 헤더도 허용하려면:
            if ((token == null || token.isBlank())) {
                String auth = httpRequest.getHeader("Authorization");
                if (auth != null && auth.startsWith("Bearer ")) {
                    token = auth.substring(7);
                }
            }

            String masked = (token == null || token.length() < 8)
                ? String.valueOf(token)
                : token.substring(0, 4) + "..." + token.substring(token.length() - 4);
            log.info("🔥 WS Handshake: token(masked)={}, roomId={}", masked, roomId);

            if (token == null || roomId == null) {
                log.warn("❌ WebSocket 인증 실패: 토큰 없음");
                return false;
            }

            if (!jwtService.validateToken(token)) {
                log.warn("❌ WebSocket 인증 실패: 유효하지 않은 토큰");
                return false;
            }

            if (jwtService.isTokenBlacklisted(token)) {
                log.warn("❌ WebSocket 인증 실패: 블랙리스트에 등록된 토큰 (로그아웃 상태)");
                return false;
            }

            // ✅ 세션 속성 저장 (Disconnect에서 사용)
            String memberId = String.valueOf(jwtService.extractIdFromToken(token));
            attributes.put("memberId", memberId);
            attributes.put("roomId", roomId);

            log.info("✅ WebSocket 인증 성공: memberId={}, roomId={}", memberId, roomId);
            return super.beforeHandshake(request, response, wsHandler, attributes);
        }

        log.warn("❌ WebSocket 인증 실패: 요청 객체가 HttpServletRequest 아님");
        return false;
    }

}
