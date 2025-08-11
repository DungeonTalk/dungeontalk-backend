package org.com.dungeontalk.global.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();      // JavaTimeModule 포함

    // feature flag 예시
    private final boolean enableAiChannel = false; // 환경변수/설정으로 주입 추천

    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody());
        String topic = new String(message.getChannel());

        // chatroom.{roomId} 형태 가드
        final String prefix = "chatroom.";
        if (!topic.startsWith(prefix)) {
            log.debug("Ignore message from topic: {}", topic);
            return;
        }
        String roomId = topic.substring(prefix.length());

        try {
            Object json = objectMapper.readValue(payload, Object.class);

            // 로그 형식 간편하게 볼 수 있도록
            if (log.isInfoEnabled()) {
                log.info("📩 Redis 수신(roomId={}):\n{}",
                    roomId, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json));
            }

            // 기본 채널도 '객체'로 쏜다
            messagingTemplate.convertAndSend("/sub/chat/room/" + roomId, json);
        } catch (Exception e) {
            log.warn("Redis 메시지 JSON 파싱 실패(roomId={}): {}", roomId, payload, e);

            // 파싱 실패시 문자열로 전달
            messagingTemplate.convertAndSend("/sub/chat/room/" + roomId, payload);
        }
        
        // AI 채팅 시스템: 객체로 전송 (새로 추가)
        try {
            Object messageData = objectMapper.readValue(payload, Object.class);
            messagingTemplate.convertAndSend("/sub/aichat/room/" + roomId, messageData);
        } catch (Exception e) {
            log.warn("AI 채팅 메시지 처리 실패: {}", payload, e);
        }
    }

}
