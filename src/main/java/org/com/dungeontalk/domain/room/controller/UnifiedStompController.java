package org.com.dungeontalk.domain.room.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.room.dto.UnifiedMessageRequest;
import org.com.dungeontalk.domain.room.service.UnifiedStompService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.stereotype.Controller;

import jakarta.validation.Valid;

/**
 * 통합된 STOMP 컨트롤러
 * AI 채팅과 플레이어 채팅의 WebSocket 메시지를 통합 처리
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class UnifiedStompController {

    private final UnifiedStompService unifiedStompService;

    /**
     * 통합된 메시지 전송 엔드포인트
     * 
     * 클라이언트는 /pub/room/{roomType}/send 로 메시지를 발행
     * roomType: "ai" (AI 게임) 또는 "chat" (플레이어 채팅)
     * 
     * 예시:
     * - AI 게임 메시지: /pub/room/ai/send
     * - 플레이어 채팅: /pub/room/chat/send
     */
    @MessageMapping("/room/{roomType}/send")
    public void sendMessage(@DestinationVariable String roomType, 
                          @Valid @Payload UnifiedMessageRequest request, HttpSession session) {
        try {
            unifiedStompService.sendMessage(roomType, request);
        } catch (Exception e) {
            log.error("메시지 전송 실패: roomType={}, error={}", roomType, e.getMessage());
            // TODO: 클라이언트에게 에러 메시지 전송
        }
    }

    /**
     * 통합된 룸 입장 엔드포인트
     * 
     * 클라이언트는 /pub/room/{roomType}/join 으로 입장 요청을 발행
     */
    @MessageMapping("/room/{roomType}/join")
    public void joinRoom(@DestinationVariable String roomType,
                        @Valid @Payload UnifiedMessageRequest request, HttpSession session) {
        try {
            unifiedStompService.joinRoom(roomType, request);
        } catch (Exception e) {
            log.error("룸 입장 실패: roomType={}, error={}", roomType, e.getMessage());
        }
    }

    /**
     * 통합된 룸 퇴장 엔드포인트
     * 
     * 클라이언트는 /pub/room/{roomType}/leave 로 퇴장 요청을 발행
     */
    @MessageMapping("/room/{roomType}/leave")
    public void leaveRoom(@DestinationVariable String roomType,
                         @Valid @Payload UnifiedMessageRequest request, HttpSession session) {
        try {
            unifiedStompService.leaveRoom(roomType, request);
        } catch (Exception e) {
            log.error("룸 퇴장 실패: roomType={}, error={}", roomType, e.getMessage());
        }
    }

    // === AI 게임 전용 엔드포인트 (하위 호환성) ===

    /**
     * AI 게임 턴 시작 (AI 게임 전용)
     */
    @MessageMapping("/room/ai/turn/start")
    public void startTurn(@Valid @Payload UnifiedMessageRequest request) {
        try {
            unifiedStompService.startTurn(request);
        } catch (Exception e) {
            log.error("AI 게임 턴 시작 실패: roomId={}, error={}", request.getRoomId(), e.getMessage());
        }
    }

    /**
     * AI 게임 턴 종료 (AI 게임 전용)
     */
    @MessageMapping("/room/ai/turn/end")
    public void endTurn(@Valid @Payload UnifiedMessageRequest request) {
        try {
            unifiedStompService.endTurn(request);
        } catch (Exception e) {
            log.error("AI 게임 턴 종료 실패: roomId={}, error={}", request.getRoomId(), e.getMessage());
        }
    }
}