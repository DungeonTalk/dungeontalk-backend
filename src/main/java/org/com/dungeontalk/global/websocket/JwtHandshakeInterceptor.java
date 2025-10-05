package org.com.dungeontalk.global.websocket;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.global.security.JwtProvider;
import org.com.dungeontalk.global.security.JwtRedisService;
import org.com.dungeontalk.global.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor extends HttpSessionHandshakeInterceptor {

    private final JwtService jwtService;
    private final JwtProvider jwtProvider;
    private final JwtRedisService jwtRedisService;

    public boolean beforeHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Map<String, Object> attributes) throws Exception {

        if (!(request instanceof ServletServerHttpRequest servlet)) {
            log.warn("❌ WS 인증 실패: 요청이 ServletServerHttpRequest 아님");
            return false;
        }

        HttpServletRequest http = servlet.getServletRequest();

        // 1) 토큰/roomId 추출 (query → Authorization: Bearer)
        String token = http.getParameter("token");
        String roomId = http.getParameter("roomId");

        if (token == null || token.isBlank()) {
            String auth = http.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                token = auth.substring(7);
            }
        }

        String masked = (token == null || token.length() < 8)
            ? String.valueOf(token)
            : token.substring(0, 4) + "..." + token.substring(token.length() - 4);
        log.info("🔥 WS Handshake: token(masked)={}, roomId={}", masked, roomId);

        // 2) 필수 파라미터 확인
        if (token == null || token.isBlank() || roomId == null || roomId.isBlank()) {
            ((ServletServerHttpResponse) response).getServletResponse().setStatus(HttpStatus.BAD_REQUEST.value());
            log.warn("❌ WS 인증 실패: token/roomId 누락");
            return false;
        }

        // 3) 서명/만료 검증
        if (!jwtProvider.validateToken(token)) {
            ((ServletServerHttpResponse) response).getServletResponse().setStatus(HttpStatus.UNAUTHORIZED.value());
            log.warn("❌ WS 인증 실패: 토큰 검증 실패");
            return false;
        }

        // 4) 블랙리스트(로그아웃/취소) 확인 — ✅ 버그 수정: 블랙리스트에 **있으면** 차단
        if (jwtRedisService.isTokenBlacklisted(token)) {
            ((ServletServerHttpResponse) response).getServletResponse().setStatus(HttpStatus.UNAUTHORIZED.value());
            log.warn("❌ WS 인증 실패: 블랙리스트 토큰");
            return false;
        }

        // 5) 클레임에서 memberId 추출
        String memberId;
        try {
            memberId = String.valueOf(jwtService.extractIdFromToken(token));
        } catch (Exception e) {
            log.warn("❌ WS 인증 실패: 토큰에서 memberId 추출 실패", e);
            return false;
        }

        // 6) 세션 속성 저장 (disconnect 핸들러에서 사용)
        attributes.put("memberId", memberId);
        attributes.put("roomId", roomId);

        log.info("✅ WS 인증 성공: memberId={}, roomId={}", memberId, roomId);
        return super.beforeHandshake(request, response, wsHandler, attributes);
    }

}
