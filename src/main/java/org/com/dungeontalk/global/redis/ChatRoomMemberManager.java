package org.com.dungeontalk.global.redis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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

        // 닉네임은 항상 최신으로 동기화
        redisTemplate.opsForHash().put(hashKey(roomId), memberId, nickname != null ? nickname : "");
        return added != null && added > 0; // true면 최초 입장
    }

    // 유저 퇴장 (멱등) : SREM 결과가 1일 때만 '실제 퇴장'
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

    // 현재 유저 목록 (memberId -> nickname) — 순서 보존 & null 안전
    public Map<String,String> getOnlineNickMap(String roomId){
        Set<String> idSet = redisTemplate.opsForSet().members(setKey(roomId));
        if (idSet == null || idSet.isEmpty()) return Collections.emptyMap();

        // Set은 순서 미보장 → 리스트로 고정하여 HMGET 결과와 인덱스 매칭
        List<String> ids = new ArrayList<>(idSet);
        List<Object> nickObjs = redisTemplate.opsForHash().multiGet(
            hashKey(roomId), new ArrayList<>(ids));

        Map<String,String> map = new LinkedHashMap<>(); // 입력 순서 보존
        for (int i = 0; i < ids.size(); i++) {
            Object o = (nickObjs != null && i < nickObjs.size()) ? nickObjs.get(i) : null;
            map.put(ids.get(i), o != null ? String.valueOf(o) : "");
        }

        return map;
    }

    // (유지) 기존 메서드와의 호환이 필요하면 아래 3개는 놔둬도 OK
    public void clearRoom(String roomId){
        redisTemplate.delete(setKey(roomId));
        redisTemplate.delete(hashKey(roomId));
    }

}
