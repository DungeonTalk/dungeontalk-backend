package org.com.dungeontalk.global.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
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
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ObjectWriter prettyPrinter = objectMapper.writerWithDefaultPrettyPrinter();

    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody());
        String topic = new String(message.getChannel());

        try {
            Object json = objectMapper.readValue(payload, Object.class);
            String prettyPayload = prettyPrinter.writeValueAsString(json);
            log.info("📩 Redis pub/sub 수신 메시지:\n{}", prettyPayload);
        } catch (Exception e) {
            log.warn("Redis 메시지 JSON 파싱 실패: {}", payload, e);
        }

        // roomId 추출 (chatroom.{roomId})
        String roomId = topic.substring("chatroom.".length());

        // 기존 채팅 시스템: 문자열로 전송 (기존 방식 유지)
        messagingTemplate.convertAndSend("/sub/chat/room/" + roomId, payload);
        
        // AI 채팅 시스템: 객체로 전송 (새로 추가)
        try {
            Object messageData = objectMapper.readValue(payload, Object.class);
            messagingTemplate.convertAndSend("/sub/aichat/room/" + roomId, messageData);
        } catch (Exception e) {
            log.warn("AI 채팅 메시지 처리 실패: {}", payload, e);
        }
    }

}
