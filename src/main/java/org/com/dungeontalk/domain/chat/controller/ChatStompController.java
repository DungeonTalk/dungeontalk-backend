package org.com.dungeontalk.domain.chat.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.chat.dto.request.ChatMessageSendRequestDto;
import org.com.dungeontalk.domain.chat.service.ChatMessageService;
import org.springframework.messaging.handler.annotation.MessageMapping;
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
    public void sendMessage(ChatMessageSendRequestDto dto) throws JsonProcessingException {
        log.debug("STOMP 수신 메시지 처리 요청: {}", objectMapper.writeValueAsString(dto));
        chatMessageService.processMessage(dto);
    }

}
