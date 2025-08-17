package org.com.dungeontalk.domain.aichat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.aichat.dto.request.DiceRollRequest;
import org.com.dungeontalk.domain.aichat.service.AiGameFlowService;
import org.com.dungeontalk.global.rsData.RsData;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI 게임 액션", description = "AI 게임 내 상호작용 관련 API")
@RestController
@RequestMapping("/v1/aichat/rooms/{roomId}/action")
@RequiredArgsConstructor
@Slf4j
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

    @Operation(summary = "스탯 보정치 적용 d20 주사위 굴림", description = "캐릭터의 스탯 보정치가 적용된 d20 주사위를 굴립니다.")
    @PostMapping("/roll-stat")
    public RsData<String> rollWithStat(
            @PathVariable String roomId,
            @RequestBody DiceRollRequest request) {
        
        if (request.getStatType() == null || request.getStatType().isBlank()) {
            return RsData.of("400", "스탯 타입을 지정해주세요. (str, dex, int, wis, wil, luk)", null);
        }
        
        try {
            aiGameFlowService.processDiceRollWithStat(
                roomId, 
                request.getMemberId(), 
                request.getStatType(),
                request.getAction()
            );
            
            return RsData.of("200", "스탯 기반 주사위 굴림이 처리되었습니다.", null);
            
        } catch (IllegalArgumentException e) {
            return RsData.of("400", "잘못된 요청: " + e.getMessage(), null);
        } catch (Exception e) {
            log.error("스탯 기반 주사위 굴림 처리 실패: roomId={}, memberId={}, statType={}", 
                     roomId, request.getMemberId(), request.getStatType(), e);
            return RsData.of("500", "주사위 굴림 처리 중 오류가 발생했습니다.", null);
        }
    }
}
