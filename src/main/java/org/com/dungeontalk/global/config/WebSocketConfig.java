package org.com.dungeontalk.global.config;

import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.global.websocket.JwtHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final ThreadPoolTaskScheduler stompTaskScheduler;

    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 메시지를 받을 경로
        registry.enableSimpleBroker("/sub")
            .setHeartbeatValue(new long[]{10_000, 10_000})
            .setTaskScheduler(stompTaskScheduler);

        // 메시지를 보낼 경로
        registry.setApplicationDestinationPrefixes("/pub");
    }

    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 연결 경로
        registry.addEndpoint("/ws-chat")
            .addInterceptors(jwtHandshakeInterceptor)           // WebSocket JWT 인증
            .setAllowedOriginPatterns("*");

        // SockJS 사용 시에도 Heartbeat 간격 힌트를 줄 수 있음(서버-전송층 수준)
        registry.addEndpoint("/ws-chat")
            .addInterceptors(jwtHandshakeInterceptor)
            .setAllowedOriginPatterns("*")
            .withSockJS()                       // SockJS 지원
            .setHeartbeatTime(10_000);          // SockJS 전송층 heartbeat
    }

    //  전송 채널 튜닝 — 느린 네트워크/탭 슬립 시 안정성 개선
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setSendTimeLimit(20_000)
            .setSendBufferSizeLimit(512 * 1024)
            .setMessageSizeLimit(128 * 1024);
    }

}
