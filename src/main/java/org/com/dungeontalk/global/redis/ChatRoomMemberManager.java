package org.com.dungeontalk.global.redis;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatRoomMemberManager {

    private final StringRedisTemplate redisTemplate;

    private String setKey(String roomId){ return "chat:room:%s:users".formatted(roomId); }
    private String hashKey(String roomId){ return "chat:room:%s:nicks".formatted(roomId); }

    // 유저 입장 (idempotent) : SADD 결과가 1일 때만 '실제 입장'
    public boolean addUser(String roomId, String memberId, String nickname){
        Long added = redisTemplate.opsForSet().add(setKey(roomId), memberId);
        redisTemplate.opsForHash().put(hashKey(roomId), memberId, nickname != null ? nickname : "");
        return added != null && added > 0; // true면 최초 입장
    }

    // 유저 퇴장 (idempotent) : SREM 결과가 1일 때만 '실제 퇴장'
    public boolean removeUser(String roomId, String memberId){
        Long removed = redisTemplate.opsForSet().remove(setKey(roomId), memberId);
        redisTemplate.opsForHash().delete(hashKey(roomId), memberId);
        return removed != null && removed > 0; // true면 실제로 빠짐
    }

    // 현재 유저 수
    public long getUserCount(String roomId){
        Long n = redisTemplate.opsForSet().size(setKey(roomId));
        return n != null ? n : 0L;
    }

    // 현재 유저 목록 (memberId -> nickname)
    public Map<String,String> getOnlineNickMap(String roomId){
        Set<String> ids = redisTemplate.opsForSet().members(setKey(roomId));
        if (ids == null || ids.isEmpty()) return Map.of();

        List<Object> nickObjs = redisTemplate.opsForHash().multiGet(hashKey(roomId), ids.stream().map(Object.class::cast).toList());
        Map<String,String> map = new java.util.HashMap<>();
        int i=0; for (String id: ids) map.put(id, String.valueOf(nickObjs.get(i++)));
        return map;
    }

    // (유지) 기존 메서드와의 호환이 필요하면 아래 3개는 놔둬도 OK
    public void clearRoom(String roomId){
        redisTemplate.delete(setKey(roomId));
        redisTemplate.delete(hashKey(roomId));
    }

}
