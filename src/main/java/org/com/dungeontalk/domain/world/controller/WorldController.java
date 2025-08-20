package org.com.dungeontalk.domain.world.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.world.dto.response.WorldResponse;
import org.com.dungeontalk.domain.world.service.WorldService;
import org.com.dungeontalk.global.rsData.RsData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "월드", description = "월드 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/worlds")
public class WorldController {

    private final WorldService worldService;

    @Operation(summary = "모든 월드 조회", description = "모든 월드의 리스트를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "월드 리스트 조회 성공",
                    content = @Content(schema = @Schema(implementation = WorldResponse.class)))
    })
    @GetMapping
    public RsData<List<WorldResponse>> getAllWorlds() {
        List<WorldResponse> worlds = worldService.getAllWorlds();
        return RsData.of("200", "성공적으로 모든 월드를 불러왔습니다.", worlds);
    }
}
