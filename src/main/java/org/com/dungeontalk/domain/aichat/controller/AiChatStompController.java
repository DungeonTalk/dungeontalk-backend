package org.com.dungeontalk.domain.aichat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.aichat.common.AiMessageType;
import org.com.dungeontalk.domain.aichat.dto.request.AiGameMessageSendRequest;
import org.com.dungeontalk.domain.aichat.dto.request.AiGenerateRequest;
import org.com.dungeontalk.domain.aichat.service.AiGameMessageService;
import org.com.dungeontalk.global.rsData.RsData;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AiChatStompController {

    private final AiGameMessageService aiGameMessageService;

    /**
     * 클라이언트로부터 AI 채팅 메시지를 수신하는 엔드포인트
     * 
     * 클라이언트는 /pub/aichat/send 로 메시지를 발행한다.
     * 해당 메시지는 @MessageMapping("/aichat/send")으로 매핑된다.
     * AI 채팅은 턴제로 동작하므로 AI 응답 중에는 메시지 전송이 차단된다.
     */
    @MessageMapping("/aichat/send")
    public RsData<String> sendMessage(@Payload AiGameMessageSendRequest request) {
        return aiGameMessageService.handleWebSocketMessage(request);
    }

    /**
     * 게임방 입장을 위한 엔드포인트
     */
    @MessageMapping("/aichat/join")
    public RsData<String> joinRoom(@Payload AiGameMessageSendRequest request) {
        return aiGameMessageService.handleJoinRoom(request);
    }

    /**
     * 게임방 퇴장을 위한 엔드포인트
     */
    @MessageMapping("/aichat/leave")
    public RsData<String> leaveRoom(@Payload AiGameMessageSendRequest request) {
        return aiGameMessageService.handleLeaveRoom(request);
    }

    /**
     * 턴 시작을 위한 엔드포인트
     * 
     * 시스템이 새로운 턴을 시작할 때 사용한다.
     * 일반적으로 게임 로직에서 호출되며, 플레이어에게 턴 시작을 알린다.
     */
    @MessageMapping("/aichat/turn/start")
    public RsData<String> startTurn(@Payload AiGameMessageSendRequest request) {
        return aiGameMessageService.handleStartTurn(request);
    }

    /**
     * 턴 종료를 위한 엔드포인트
     * 
     * AI 응답 완료 후 턴 종료를 알리는데 사용한다.
     * AI 서비스에서 응답 생성 완료 후 호출된다.
     */
    @MessageMapping("/aichat/turn/end")
    public RsData<String> endTurn(@Payload AiGameMessageSendRequest request) {
        return aiGameMessageService.handleEndTurn(request);
    }
}