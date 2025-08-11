package org.com.dungeontalk.domain.aichat.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.aichat.util.AiChatErrorHandler;
import org.com.dungeontalk.domain.aichat.util.AiChatLogUtils;
import org.com.dungeontalk.domain.aichat.dto.request.AiGameMessageSendRequest;
import org.com.dungeontalk.domain.aichat.service.AiGameMessageService;
import org.com.dungeontalk.domain.aichat.service.AiGameStateService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AiChatStompController {

    private final AiGameMessageService aiGameMessageService;
    private final AiGameStateService aiGameStateService;
    private final AiChatErrorHandler errorHandler;

    /**
     * 클라이언트로부터 AI 채팅 메시지를 수신하는 엔드포인트
     * 
     * 클라이언트는 /pub/aichat/send 로 메시지를 발행한다.
     * 해당 메시지는 @MessageMapping("/aichat/send")으로 매핑된다.
     * AI 채팅은 턴제로 동작하므로 AI 응답 중에는 메시지 전송이 차단된다.
     */
    @MessageMapping("/aichat/send")
    public void sendMessage(@Payload AiGameMessageSendRequest request) throws JsonProcessingException {
        AiChatLogUtils.logGameActionStart("AI 채팅 메시지 수신", request.getAiGameRoomId());

        errorHandler.executeWithLogging(() -> {
            // AI 응답 처리 중인지 확인
            if (aiGameStateService.isAiProcessing(request.getAiGameRoomId())) {
                log.warn("AI 응답 처리 중이므로 메시지 전송 차단: roomId={}", request.getAiGameRoomId());
                return;
            }

            // 세션 유효성 검증
            if (!aiGameStateService.isSessionValid(request.getAiGameRoomId())) {
                log.warn("유효하지 않은 게임 세션: roomId={}", request.getAiGameRoomId());
                return;
            }

            // 메시지 처리
            aiGameMessageService.processMessage(request);

            // 세션 만료 시간 연장
            aiGameStateService.extendSession(request.getAiGameRoomId());

            AiChatLogUtils.logGameAction("AI 채팅 메시지 처리", request.getAiGameRoomId(), 
                                      request.getSenderId(), request.getMessageType());
        }, "AI 채팅 메시지 처리", request.getAiGameRoomId());
    }

    /**
     * 게임방 입장을 위한 엔드포인트
     * 
     * 클라이언트는 /pub/aichat/join 으로 입장 요청을 발행한다.
     * 입장 시 게임 세션이 시작되고 시스템 메시지가 전송된다.
     */
    @MessageMapping("/aichat/join")
    public void joinRoom(@Payload AiGameMessageSendRequest request) {
        aiGameMessageService.handleJoinRoom(request);
    }

    /**
     * 게임방 퇴장을 위한 엔드포인트
     * 
     * 클라이언트는 /pub/aichat/leave 로 퇴장 요청을 발행한다.
     * 퇴장 시 시스템 메시지가 전송되고 필요시 게임이 종료된다.
     */
    @MessageMapping("/aichat/leave")
    public void leaveRoom(@Payload AiGameMessageSendRequest request) {
        aiGameMessageService.handleLeaveRoom(request);
    }

    /**
     * 턴 시작을 위한 엔드포인트
     * 
     * 시스템이 새로운 턴을 시작할 때 사용한다.
     * 일반적으로 게임 로직에서 호출되며, 플레이어에게 턴 시작을 알린다.
     */
    @MessageMapping("/aichat/turn/start")
    public void startTurn(@Payload AiGameMessageSendRequest request) {
        aiGameMessageService.handleStartTurn(request);
    }

    /**
     * 턴 종료를 위한 엔드포인트
     * 
     * AI 응답 완료 후 턴 종료를 알리는데 사용한다.
     * AI 서비스에서 응답 생성 완료 후 호출된다.
     */
    @MessageMapping("/aichat/turn/end")
    public void endTurn(@Payload AiGameMessageSendRequest request) {
        aiGameMessageService.handleEndTurn(request, aiGameStateService);
    }
}