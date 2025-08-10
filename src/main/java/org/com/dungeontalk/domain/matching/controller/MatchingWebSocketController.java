package org.com.dungeontalk.domain.matching.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.matching.dto.request.MatchingJoinRequest;
import org.com.dungeontalk.domain.matching.dto.request.MatchingCancelRequest;
import org.com.dungeontalk.domain.matching.service.MatchingService;
import org.com.dungeontalk.domain.matching.service.MatchingWebSocketService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MatchingWebSocketController {

    private final MatchingService matchingService;
    private final MatchingWebSocketService webSocketService;

    /**
     * WebSocket을 통한 매칭 참가 처리
     */
    @MessageMapping("/matching/join")
    @SendToUser("/sub/matching/user")
    public void joinMatching(@Payload MatchingJoinRequest request) {
        log.info("WebSocket 매칭 참가 요청: userId={}, worldType={}", request.getUserId(), request.getWorldType());

        try {
            matchingService.joinMatching(request.getUserId(), request.getWorldType());
            
            // WebSocket으로 상태 업데이트는 MatchingService 내부에서 처리
            
        } catch (Exception e) {
            log.error("WebSocket 매칭 참가 중 오류: userId={}", request.getUserId(), e);
            webSocketService.sendError(request.getUserId(), e.getMessage());
        }
    }

    /**
     * WebSocket을 통한 매칭 취소 처리
     */
    @MessageMapping("/matching/cancel")
    @SendToUser("/sub/matching/user")
    public void cancelMatching(@Payload MatchingCancelRequest request) {
        log.info("WebSocket 매칭 취소 요청: userId={}", request.getUserId());

        try {
            matchingService.cancelMatching(request.getUserId());
            
        } catch (Exception e) {
            log.error("WebSocket 매칭 취소 중 오류: userId={}", request.getUserId(), e);
            webSocketService.sendError(request.getUserId(), e.getMessage());
        }
    }
}