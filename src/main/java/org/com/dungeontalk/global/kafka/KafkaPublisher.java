package org.com.dungeontalk.global.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.chat.regular}")
    private String chatTopic;

    /**
     * 채팅 메시지 비동기 발행
     * @param roomId 채팅방 ID (파티션 키)
     * @param message 메시지 객체
     * @return CompletableFuture
     */
    @Async("chatKafkaExecutor")
    public CompletableFuture<Void> publishChatAsync(String roomId, Object message) {
        return kafkaTemplate.send(chatTopic, roomId, message)
            .thenAccept(result -> {
                log.debug("Kafka 메시지 발행 완료: topic={}, partition={}, offset={}, roomId={}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset(),
                    roomId);
            })
            .exceptionally(ex -> {
                log.error("Kafka 메시지 발행 실패: roomId={}, error={}", roomId, ex.getMessage(), ex);
                return null;
            });
    }
}
