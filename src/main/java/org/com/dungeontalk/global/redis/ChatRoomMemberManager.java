package org.com.dungeontalk.global.redis;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatRoomMemberManager {

    private final StringRedisTemplate redisTemplate;

    private String getRoomKey(String roomId) {
        return "chat:room:" + roomId + ":users";
    }

    // 유저 입장
    public boolean addUser(String roomId, String nickName) {
        String key = getRoomKey(roomId);
        Long addedCount = redisTemplate.opsForSet().add(key, nickName);
        return addedCount != null && addedCount > 0;
    }

    // 유저 퇴장
    public void removeUser(String roomId, String nickName) {
        redisTemplate.opsForSet().remove(getRoomKey(roomId), nickName);
    }

    // 현재 유저 목록
    public Set<String> getUserList(String roomId) {
        return redisTemplate.opsForSet().members(getRoomKey(roomId));
    }

    // 현재 유저 수
    public Long getUserCount(String roomId) {
        Long size = redisTemplate.opsForSet().size(getRoomKey(roomId));
        return size != null ? size.intValue() : 0L;
    }

    // 채팅방 초기화
    public void clearRoom(String roomId) {
        redisTemplate.delete(getRoomKey(roomId));
    }

}
