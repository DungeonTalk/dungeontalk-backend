package org.com.dungeontalk.domain.matching.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.matching.dto.request.MatchingJoinRequest;
import org.com.dungeontalk.domain.matching.dto.request.MatchingCancelRequest;
import org.com.dungeontalk.domain.matching.service.MatchingService;
import org.com.dungeontalk.domain.matching.service.WorldTypeCompatService;
import org.com.dungeontalk.domain.worldtype.entity.WorldType;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MatchingWebSocketController {

    private final MatchingService matchingService;
    private final WorldTypeCompatService worldTypeCompatService;

    /**
     * WebSocket을 통한 매칭 참가 처리
     */
    @MessageMapping("/matching/join")
    @SendToUser("/sub/matching/user")
    public void joinMatching(@Payload MatchingJoinRequest request, HttpSession session) {
        WorldType worldType = worldTypeCompatService.valueOf(request.getWorldTypeCode());
        matchingService.handleWebSocketJoinMatching(request.getMemberId(), worldType);
    }

    /**
     * WebSocket을 통한 매칭 취소 처리
     */
    @MessageMapping("/matching/cancel")
    @SendToUser("/sub/matching/user")
    public void cancelMatching(@Payload MatchingCancelRequest request, HttpSession session) {
        matchingService.handleWebSocketCancelMatching(request.getMemberId());
    }
}