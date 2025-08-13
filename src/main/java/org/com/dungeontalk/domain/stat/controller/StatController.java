package org.com.dungeontalk.domain.stat.controller;

import org.com.dungeontalk.domain.stat.dto.response.CalculatedStatsResponse;
import org.com.dungeontalk.domain.stat.service.StatAggregateService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/stat")
public class StatController {

    private final StatAggregateService statAggregateService;

    public StatController(StatAggregateService statAggregateService) {
        this.statAggregateService = statAggregateService;
    }

    @GetMapping("/{characterId}/calculated")
    public CalculatedStatsResponse calculated(@PathVariable UUID characterId) {
        var map = statAggregateService.calculateAllStats(characterId.toString()); // 리포/서비스가 String이면 변환
        return CalculatedStatsResponse.fromMap(characterId.toString(), map);      // DTO가 String이면 변환
    }
}
