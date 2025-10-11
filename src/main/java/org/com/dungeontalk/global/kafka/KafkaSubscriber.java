package org.com.dungeontalk.global.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaSubscriber {

    private final SimpMessageSendingOperations messagingTemplate;

    /**
     * 채팅 메시지 Consumer
     * - Consumer Group: dungeontalk-chat-consumer
     * - 메시지를 받아서 WebSocket으로 브로드캐스트
     */
    @KafkaListener(
        topics = "${kafka.topics.chat.regular}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeChat(ConsumerRecord<String, Object> record) {
        String roomId = record.key();
        Object message = record.value();

        try {
            log.debug("Kafka 메시지 수신: topic={}, partition={}, offset={}, roomId={}",
                record.topic(), record.partition(), record.offset(), roomId);

            // WebSocket으로 브로드캐스트
            String destination = "/sub/chat/room/" + roomId;
            messagingTemplate.convertAndSend(destination, message);

            log.debug("WebSocket 브로드캐스트 완료: destination={}, roomId={}", destination, roomId);

        } catch (Exception e) {
            log.error("메시지 처리 실패: roomId={}, error={}", roomId, e.getMessage(), e);
            // 에러 발생 시 로깅만 하고 계속 진행 (메시지 손실 방지)
        }
    }
}
