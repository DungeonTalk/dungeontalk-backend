package org.com.dungeontalk.global.websocket;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.chat.service.ChatRoomService;
import org.com.dungeontalk.global.redis.ChatRoomMemberManager;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketDisconnectHandler implements ApplicationListener<SessionDisconnectEvent> {

    private final ChatRoomMemberManager chatRoomMemberManager;
    private final ChatRoomService chatRoomService;

    @Override
    public void onApplicationEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String memberId = (String) Objects.requireNonNull(accessor.getSessionAttributes()).get("memberId");
        String roomId = (String) accessor.getSessionAttributes().get("roomId");

        if (memberId != null && roomId != null) {
            log.info("🚪WebSocket 연결 끊김 감지 → memberId: {}, roomId: {}", memberId, roomId);
            chatRoomMemberManager.removeUser(roomId, memberId);
            chatRoomService.leaveRoom(roomId, memberId);
        } else {
            log.warn("WebSocket 연결 끊김: memberId 또는 roomId 누락됨 (무시됨)");
        }
    }
}
