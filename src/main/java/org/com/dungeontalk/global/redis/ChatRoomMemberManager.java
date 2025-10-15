package org.com.dungeontalk.global.redis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.chat.dto.ChatSessionDto;
import org.com.dungeontalk.domain.chat.service.ChatSessionService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 채팅방 멤버 관리 (세션 기반으로 리팩토링)
 * 기존 Set/Hash 기반 관리에서 ChatSessionService 기반으로 전환
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomMemberManager {

    private final StringRedisTemplate redisTemplate;
    private final ChatSessionService chatSessionService;

    private String setKey(String roomId){ return "chat:room:%s:users".formatted(roomId); }
    private String hashKey(String roomId){ return "chat:room:%s:nicks".formatted(roomId); }

    // 유저 입장 (idempotent) : SADD 결과가 1일 때만 '실제 입장'
    public boolean addUser(String roomId, String memberId, String nickname) {
        Long added = redisTemplate.opsForSet().add(setKey(roomId), memberId);

        // 닉네임은 항상 최신으로 동기화
        redisTemplate.opsForHash().put(hashKey(roomId), memberId, nickname != null ? nickname : "");
        return added != null && added > 0; // true면 최초 입장
    }

    // 유저 퇴장 (멱등) : SREM 결과가 1일 때만 '실제 퇴장'
    public boolean removeUser(String roomId, String memberId) {
        Long removed = redisTemplate.opsForSet().remove(setKey(roomId), memberId);
        redisTemplate.opsForHash().delete(hashKey(roomId), memberId);
        return removed != null && removed > 0; // true면 실제로 빠짐
    }

    // 현재 유저 수 (세션 기반)
    public long getUserCount(String roomId) {
        List<ChatSessionDto> sessions = chatSessionService.getActiveSessionsInRoom(roomId);
        return sessions.size();
    }

    // 현재 유저 목록 (memberId -> nickname) — 세션 기반으로 조회
    public Map<String, String> getOnlineNickMap(String roomId) {
        // 세션 서비스에서 활성 세션 조회
        List<ChatSessionDto> sessions = chatSessionService.getActiveSessionsInRoom(roomId);

        if (sessions.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> map = new LinkedHashMap<>(); // 입력 순서 보존
        for (ChatSessionDto session : sessions) {
            map.put(session.getMemberId(), session.getNickname() != null ? session.getNickname() : "");
        }

        return map;
    }

    // 채팅방 전체 정리 (세션 포함)
    public void clearRoom(String roomId) {
        // 기존 Set/Hash 정리
        redisTemplate.delete(setKey(roomId));
        redisTemplate.delete(hashKey(roomId));

        // 세션 정리
        chatSessionService.endAllSessionsInRoom(roomId);

        log.info("채팅방 전체 정리 완료: roomId={}", roomId);
    }

}
