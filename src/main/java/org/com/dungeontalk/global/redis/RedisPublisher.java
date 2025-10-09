package org.com.dungeontalk.global.redis;

import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPublisher {

    @Qualifier("objectRedisTemplate")
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 일반 채팅방 메시지 비동기 발행
     * @param roomId 채팅방 ID
     * @param message 발행할 메시지 (JSON 문자열)
     * @return CompletableFuture (비동기 처리 결과)
     */
    @Async("chatRedisExecutor")
    public CompletableFuture<Void> publishAsync(String roomId, String message) {
        try {
            redisTemplate.convertAndSend("chatroom." + roomId, message);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("채팅방 메시지 발행 실패 (chatroom.{}): {}", roomId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 기존 동기 방식 유지 (하위 호환성)
     * @deprecated 새로운 코드에서는 publishAsync 사용 권장
     */
    @Deprecated
    public void publish(String roomId, String message) {
        redisTemplate.convertAndSend("chatroom." + roomId, message);
    }

    /**
     * AI 채팅 메시지 발행 (동기 - aichat 도메인용)
     */
    public void publishAiChat(String aiGameRoomId, String message) {
        redisTemplate.convertAndSend("aichat." + aiGameRoomId, message);
    }

}
