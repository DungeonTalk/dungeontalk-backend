package org.com.dungeontalk.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(String roomId, String message) {
        redisTemplate.convertAndSend("chatroom." + roomId, message);
    }

}
