package org.com.dungeontalk.domain.aichat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.aichat.dto.request.DiceRollRequest;
import org.com.dungeontalk.domain.aichat.service.AiGameFlowService;
import org.com.dungeontalk.global.rsData.RsData;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI 게임 액션", description = "AI 게임 내 상호작용 관련 API")
@RestController
@RequestMapping("/v1/aichat/rooms/{roomId}/action")
@RequiredArgsConstructor
public class AiGameActionController {

    private final AiGameFlowService aiGameFlowService;

    @Operation(summary = "d20 주사위 굴림 (MVP)", description = "캐릭터가 d20 주사위를 굴립니다.")
    @PostMapping("/roll-d20")
    public RsData<String> rollD20(
            @PathVariable String roomId,
            @RequestBody DiceRollRequest request) {
        
        // MVP에서는 d20 굴림만 처리
        aiGameFlowService.processDiceRoll(roomId, request.getMemberId(), "d20");
        
        return RsData.of("200", "주사위 굴림 요청이 처리되었습니다.", null);
    }
}
