package org.com.dungeontalk.domain.stat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(summary = "캐릭터 계산된 스탯 조회", 
        description = "캐릭터 ID로 종족별 공식에 따라 계산된 모든 스탯을 조회합니다. HP, MP, 물리공격력, 마법공격력, 회피율, 명중률, 주사위 확률이 포함됩니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "스탯 계산 성공",
            content = @Content(schema = @Schema(implementation = CalculatedStatsResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 캐릭터 ID"),
        @ApiResponse(responseCode = "404", description = "캐릭터 없음 또는 종족 스탯 공식 없음")
    })
    @GetMapping("/{characterId}/calculated")
    public RsData<CalculatedStatsResponse> calculated(@PathVariable UUID characterId) {
        var map = statAggregateService.calculateAllStats(characterId.toString()); // 리포/서비스가 String이면 변환
        var response = CalculatedStatsResponse.fromMap(characterId.toString(), map);      // DTO가 String이면 변환
        return RsData.of("200", "스탯 계산 완료", response);
    }
}
