package org.com.dungeontalk.global.websocket;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.chat.service.ChatRoomService;
import org.com.dungeontalk.domain.chat.service.ChatSessionService;
import org.com.dungeontalk.global.exception.ErrorCode;
import org.com.dungeontalk.global.exception.customException.ChatException;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketDisconnectHandler implements ApplicationListener<SessionDisconnectEvent> {

    private final ChatRoomService chatRoomService;
    private final ChatSessionService chatSessionService;  // 세션 관리 (신규)

    @Override
    public void onApplicationEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs == null) return;

        String sessionId = accessor.getSessionId();
        CloseStatus close = event.getCloseStatus();      // may be null on SockJS
        String memberId = (String) attrs.get("memberId");
        String roomId   = (String) attrs.get("roomId");
        if (memberId == null || roomId == null) return;

        // 가짜 매칭 채널은 퇴장 처리 대상 아님, matching은 채팅방 도메인이 아님!
        // 호출 자체를 피해서 불필요한 예외 로그 제거하기
        if ("matching".equals(roomId)) {
            log.info("WS disconnect (matching channel): sessionId={}, memberId={}", sessionId, memberId);
            return;
        }

        Boolean handled = (Boolean) attrs.get("leaveHandled");
        if (handled != null && handled) return;

        try {
            // 세션 종료 (명시적)
            chatSessionService.endSession(roomId, memberId);

            // 퇴장 처리 (멱등)
            chatRoomService.leaveRoom(roomId, memberId);

            log.info("WS disconnect handled: sessionId={}, memberId={}, roomId={}, close={}",
                sessionId, memberId, roomId, close);
        } catch (ChatException e) {
            // 상황별 로그 레벨 분리
            if (e.getErrorCode() == ErrorCode.CHAT_ROOM_NOT_FOUND) {
                // 방이 이미 삭제되었거나 race인 케이스 — 소음 줄이기
                log.debug("Skip leave (room not found): sessionId={}, memberId={}, roomId={}, reason={}",
                    sessionId, memberId, roomId, e.getMessage());
            } else if (e.getErrorCode() == ErrorCode.CHAT_CAPACITY_EXCEEDED) {
                // 채팅방 정원 초과인 경우
                log.warn("Leave failed (capacity state?): sessionId={}, memberId={}, roomId={}, reason={}",
                    sessionId, memberId, roomId, e.getMessage());
            } else {
                // 알 수 없는 채팅 에러는 에러 레벨로 남기되, 디스커넥트 플로우는 끊지 않음
                log.error("Leave failed: sessionId={}, memberId={}, roomId={}, reason={}",
                    sessionId, memberId, roomId, e.getMessage(), e);
            }
        } finally {
            attrs.put("leaveHandled", true);
        }
    }
}
