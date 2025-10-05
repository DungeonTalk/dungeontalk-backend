package org.com.dungeontalk.domain.chat.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.chat.dto.request.ChatMessageSendRequestDto;
import org.com.dungeontalk.domain.chat.service.ChatMessageService;
import org.com.dungeontalk.global.exception.customException.ChatException;
import org.com.dungeontalk.global.exception.dto.ErrorPayload;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatMessageService chatMessageService;
    private final ObjectMapper objectMapper;

    /**
     * 클라이언트로부터 수신한 STOMP 메시지를 처리하는 엔드포인트
     *
     * 클라이언트는 /pub/chat/send 로 메시지를 발행한다.
     * 해당 메시지는 @MessageMapping("/chat/send")으로 매핑된다.
     * 이후 ChatMessageService가 메시지의 타입에 따라 처리(JOIN, LEAVE, TALK)
     */
    @MessageMapping("/chat/send") // /pub/chat/send
    public void sendMessage(@Valid @Payload ChatMessageSendRequestDto dto) throws JsonProcessingException {
        if (log.isDebugEnabled()) {
            log.debug("STOMP 수신: {}", objectMapper.writeValueAsString(dto));
        }
        chatMessageService.processMessage(dto);
    }

    /**
     * 핵심: ChatException 발생 시 STOMP ERROR로 세션이 끊기지 않도록
     * 사용자 전용 큐(/user/queue/errors)로 소프트 에러를 전달한다.
     * 프런트에서 이 큐를 구독하지 않으면 알림 없이 연결/구독은 유지된다.
     */
    @MessageExceptionHandler(ChatException.class)
    @SendToUser("/queue/errors")
    public ErrorPayload handleChatException(ChatException e) {
        // 사용자에게는 표준 코드/메시지만 전달 (getMessage()는 [코드] prefix가 붙으므로 지양)
        String code = e.getErrorCode().getErrorCode();   // 예: "409-CH03"
        String message = e.getErrorCode().getMessage();  // 예: "채팅방 정원을 초과했습니다."
        log.warn("STOMP Soft Error -> code={}, detail={}", code, e.getMessage());
        return new ErrorPayload(code, message);
    }

}
