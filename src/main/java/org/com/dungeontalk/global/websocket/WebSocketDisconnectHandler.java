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

        if (attrs == null) return;

        String memberId = (String) attrs.get("memberId");
        String roomId   = (String) attrs.get("roomId");

        if (memberId == null || roomId == null) return;

        Boolean handled = (Boolean) attrs.get("leaveHandled");
        if (handled != null && handled) return;

        try {
            chatRoomService.leaveRoom(roomId, memberId);        // 멱등
        } finally {
            attrs.put("leaveHandled", true);
        }
    }
}
