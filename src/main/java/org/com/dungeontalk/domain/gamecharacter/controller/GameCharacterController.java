package org.com.dungeontalk.domain.gamecharacter.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.gamecharacter.dto.request.CreateCharacterRequest;
import org.com.dungeontalk.domain.gamecharacter.dto.response.GameCharacterDetailResponse;
import org.com.dungeontalk.domain.gamecharacter.dto.response.GameCharacterResponse;
import org.com.dungeontalk.domain.gamecharacter.service.GameCharacterService;
import org.com.dungeontalk.domain.stat.repository.RaceStatsRepository;
import org.com.dungeontalk.global.rsData.RsData;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/characters")
@RequiredArgsConstructor
public class GameCharacterController {

    private final GameCharacterService gameCharacterService;
    private final RaceStatsRepository raceStatsRepository;

    // 새로운 캐릭터 생성 (캐릭터 생성 화면에서 사용)
    @Operation(summary = "캐릭터 생성", description = "새로운 캐릭터를 생성합니다. 레벨 1, 모든 스탯 10으로 초기화됩니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "캐릭터 생성 성공",
            content = @Content(schema = @Schema(implementation = GameCharacterResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (존재하지 않는 종족 등)")
    })
    @PostMapping
    public RsData<GameCharacterResponse> createCharacter(@RequestBody CreateCharacterRequest request) {
        var character = gameCharacterService.createCharacter(request);
        var response = GameCharacterResponse.from(character);
        return RsData.of("201", "캐릭터 생성 완료", response);
    }

    // 특정 캐릭터 기본 정보 조회 (계산된 스탯 없이)
    @Operation(summary = "캐릭터 기본 정보 조회", description = "캐릭터 ID로 기본 정보를 조회합니다. 계산된 스탯은 포함되지 않습니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = GameCharacterResponse.class))),
        @ApiResponse(responseCode = "404", description = "캐릭터 없음")
    })
    @GetMapping("/basic/{id}")
    public RsData<GameCharacterResponse> getCharacterBasic(@PathVariable String id) {
        var character = gameCharacterService.findById(id);
        var response = GameCharacterResponse.from(character);
        return RsData.of("200", "캐릭터 조회 완료", response);
    }

    // 특정 캐릭터 상세 정보 조회 (계산된 스탯 포함)
    @Operation(summary = "캐릭터 상세 정보 조회", description = "캐릭터 ID로 상세 정보를 조회합니다. 계산된 스탯(HP, MP, 공격력 등)이 포함됩니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = GameCharacterDetailResponse.class))),
        @ApiResponse(responseCode = "404", description = "캐릭터 없음")
    })
    @GetMapping("/{id}")
    public RsData<GameCharacterDetailResponse> getCharacterDetail(@PathVariable String id) {
        var response = gameCharacterService.findDetailById(id);
        return RsData.of("200", "캐릭터 상세 조회 완료", response);
    }

    // 특정 멤버의 캐릭터 조회 (MVP: 1개 멤버당 1개 캐릭터)
    @Operation(summary = "멤버의 캐릭터 조회", description = "멤버 ID로 해당 멤버의 캐릭터를 조회합니다. MVP에서는 멤버당 1개 캐릭터만 존재합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = GameCharacterResponse.class))),
        @ApiResponse(responseCode = "404", description = "해당 멤버의 캐릭터 없음")
    })
    @GetMapping
    public RsData<GameCharacterResponse> getCharacterByMember(@RequestParam String memberId) {
        var character = gameCharacterService.findByMemberId(memberId);
        var response = GameCharacterResponse.from(character);
        return RsData.of("200", "멤버 캐릭터 조회 완료", response);
    }

    // 사용 가능한 모든 종족 목록 조회 (캐릭터 생성 시 종족 선택에서 사용)
    @Operation(summary = "종족 목록 조회", description = "캐릭터 생성 시 선택 가능한 모든 종족 목록을 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class))))
    })
    @GetMapping("/races")
    public RsData<List<String>> getRaces() {
        var races = raceStatsRepository.findAllRaceNames();
        return RsData.of("200", "종족 목록 조회 완료", races);
    }
}