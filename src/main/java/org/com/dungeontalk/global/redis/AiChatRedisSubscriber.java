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
public class AiChatRedisSubscriber implements MessageListener {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ObjectWriter prettyPrinter = objectMapper.writerWithDefaultPrettyPrinter();

    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody());
        String topic = new String(message.getChannel());

        log.info("🤖 [DEBUG] AiChatRedisSubscriber.onMessage 호출됨 - topic: {}", topic);
        log.info("🤖 [DEBUG] 원본 payload: {}", payload);

        try {
            Object json = objectMapper.readValue(payload, Object.class);
            String prettyPayload = prettyPrinter.writeValueAsString(json);
            log.info("🤖 AI 채팅 Redis pub/sub 수신 메시지:\n{}", prettyPayload);
        } catch (Exception e) {
            log.warn("AI 채팅 Redis 메시지 JSON 파싱 실패: {}", payload, e);
        }

        // roomId 추출 (aichat.{roomId})
        String roomId = topic.substring("aichat.".length());

        // AI 채팅 시스템 전용: 객체로 전송
        try {
            Object messageData = objectMapper.readValue(payload, Object.class);
            messagingTemplate.convertAndSend("/sub/aichat/room/" + roomId, messageData);
            log.debug("AI 채팅 메시지 WebSocket 브로드캐스트 완료: /sub/aichat/room/{}", roomId);
        } catch (Exception e) {
            log.error("AI 채팅 메시지 처리 실패: roomId={}, payload={}", roomId, payload, e);
        }
    }
}