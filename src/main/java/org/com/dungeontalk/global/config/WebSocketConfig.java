package org.com.dungeontalk.global.config;

import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.global.websocket.JwtHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 메시지를 받을 경로
        registry.enableSimpleBroker("/sub");

        // 메시지를 보낼 경로
        registry.setApplicationDestinationPrefixes("/pub");
    }

    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 연결 경로
        registry.addEndpoint("/ws-chat")
            .addInterceptors(jwtHandshakeInterceptor)           // WebSocket JWT 인증
            .setAllowedOriginPatterns("*")
            .withSockJS(); // SockJS 지원
    }

}
