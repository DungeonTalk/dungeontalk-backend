package org.com.dungeontalk.global.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.HashMap;
import java.util.Map;
import org.com.dungeontalk.domain.chat.service.ChatRoomService;
import org.com.dungeontalk.global.exception.ErrorCode;
import org.com.dungeontalk.global.exception.customException.ChatException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@ExtendWith(MockitoExtension.class)
class WebSocketDisconnectHandlerTest {

    @Mock
    ChatRoomService chatRoomService;

    @InjectMocks
    WebSocketDisconnectHandler handler;

    private SessionDisconnectEvent eventWithAttrs(Map<String, Object> attrs, CloseStatus status) {
        StompHeaderAccessor acc = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        acc.setSessionId("sess-1");
        acc.setSessionAttributes(attrs);

        Message<byte[]> msg = MessageBuilder.createMessage(new byte[0], acc.getMessageHeaders());

        return new SessionDisconnectEvent(this, msg, acc.getSessionId(), status);
    }

    @Test
    @DisplayName("정상: memberId/roomId 존재 → leaveRoom 호출 & leaveHandled=true")
    void ok() {
        Map<String,Object> attrs = new HashMap<>();
        attrs.put("memberId", "M-1");
        attrs.put("roomId", "room-1");

        SessionDisconnectEvent ev = eventWithAttrs(attrs, CloseStatus.NORMAL);

        handler.onApplicationEvent(ev);

        verify(chatRoomService, times(1)).leaveRoom("room-1", "M-1");
        assertThat(attrs.get("leaveHandled")).isEqualTo(true);
    }

    @Test
    @DisplayName("matching 채널은 스킵 → leaveRoom 호출 안 함")
    void skipMatchingChannel() {
        Map<String,Object> attrs = new HashMap<>();
        attrs.put("memberId", "M-1");
        attrs.put("roomId", "matching");

        SessionDisconnectEvent ev = eventWithAttrs(attrs, CloseStatus.NORMAL);

        handler.onApplicationEvent(ev);

        verifyNoInteractions(chatRoomService);
        assertThat(attrs).doesNotContainKey("leaveHandled"); // 그대로
    }

    @Test
    @DisplayName("이미 처리된 플래그 있으면 재호출 안 함")
    void alreadyHandled() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("memberId", "M-1");
        attrs.put("roomId", "room-1");
        attrs.put("leaveHandled", true);

        SessionDisconnectEvent ev = eventWithAttrs(attrs, CloseStatus.NORMAL);

        handler.onApplicationEvent(ev);

        verifyNoInteractions(chatRoomService);
    }

    @Test
    @DisplayName("채팅방을 찾을 수 없음 -> leaveHandled=true 세팅")
    void roomNotFound() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("memberId", "M-1");
        attrs.put("roomId", "room-404");

        doThrow(new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND, "gone"))
            .when(chatRoomService).leaveRoom("room-404", "M-1");

        SessionDisconnectEvent ev = eventWithAttrs(attrs, CloseStatus.NORMAL);

        handler.onApplicationEvent(ev);

        verify(chatRoomService, times(1)).leaveRoom("room-404", "M-1");
        assertThat(attrs.get("leaveHandled")).isEqualTo(true);
    }

    @Test
    @DisplayName("기타 채팅 예외도 로깅만 하고 진행 (leaveHandled=true)")
    void otherChatException() {
        Map<String,Object> attrs = new HashMap<>();
        attrs.put("memberId", "M-1");
        attrs.put("roomId", "room-1");

        doThrow(new ChatException(ErrorCode.CHAT_CAPACITY_EXCEEDED, "oops"))
            .when(chatRoomService).leaveRoom("room-1", "M-1");

        SessionDisconnectEvent ev = eventWithAttrs(attrs, CloseStatus.NORMAL);

        handler.onApplicationEvent(ev);

        verify(chatRoomService, times(1)).leaveRoom("room-1", "M-1");
        assertThat(attrs.get("leaveHandled")).isEqualTo(true);
    }

    @Test
    @DisplayName("세션 속성에 memberId/roomId 없으면 아무 것도 안 함")
    void noAttributes() {
        Map<String, Object> attrs = new HashMap<>();

        // nothing
        SessionDisconnectEvent ev = eventWithAttrs(attrs, CloseStatus.NORMAL);

        handler.onApplicationEvent(ev);

        verifyNoInteractions(chatRoomService);
    }

}