package org.com.dungeontalk.domain.stat.controller;

import org.com.dungeontalk.domain.stat.dto.response.CalculatedStatsResponse;
import org.com.dungeontalk.domain.stat.service.StatAggregateService;
import org.com.dungeontalk.global.rsData.RsData;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/stat")
public class StatController {

    private final StatAggregateService statAggregateService;

    public StatController(StatAggregateService statAggregateService) {
        this.statAggregateService = statAggregateService;
    }

    // 캐릭터의 모든 계산된 스탯 조회 (HP, MP, 공격력 등)
    @GetMapping("/{characterId}/calculated")
    public RsData<CalculatedStatsResponse> calculated(@PathVariable UUID characterId) {
        var map = statAggregateService.calculateAllStats(characterId.toString()); // 리포/서비스가 String이면 변환
        var response = CalculatedStatsResponse.fromMap(characterId.toString(), map);      // DTO가 String이면 변환
        return RsData.of("200", "스탯 계산 완료", response);
    }
}
