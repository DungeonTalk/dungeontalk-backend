package org.com.dungeontalk.global.websocket;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.chat.service.ChatRoomService;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketDisconnectHandler implements ApplicationListener<SessionDisconnectEvent> {

    private final ChatRoomService chatRoomService;

    @Override
    public void onApplicationEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> attrs = accessor.getSessionAttributes();
        String memberId = attrs != null ? (String) attrs.get("memberId") : null;
        String roomId   = attrs != null ? (String) attrs.get("roomId")   : null;

        if (memberId == null || roomId == null) return;

        // 같은 세션에서 중복 DISCONNECT가 오면 1회만 처리
        Object already = attrs.get("leaveHandled");
        if (already instanceof Boolean b && b) return;

        try {
            chatRoomService.leaveRoom(roomId, memberId);   // 멱등
        } finally {
            if (attrs != null) attrs.put("leaveHandled", true);
        }
    }
}
