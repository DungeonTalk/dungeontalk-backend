package org.com.dungeontalk.domain.gamecharacter.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.gamecharacter.dto.request.CreateCharacterRequestV2;
import org.com.dungeontalk.domain.gamecharacter.dto.request.AddExperienceRequest;
import org.com.dungeontalk.domain.gamecharacter.dto.response.GameCharacterDetailResponse;
import org.com.dungeontalk.domain.gamecharacter.dto.response.GameCharacterResponse;
import org.com.dungeontalk.domain.gamecharacter.service.GameCharacterService;
import org.com.dungeontalk.global.rsData.RsData;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.com.dungeontalk.global.security.CustomUserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 게임 캐릭터 컨트롤러 V2
 * - 인증된 사용자의 memberId를 자동으로 추출
 * - Spring Security를 통한 인증 처리
 */
@Slf4j
@Tag(name = "게임 캐릭터 V2", description = "게임 캐릭터 정보 조회 관련 API V2 (인증 기반)")
@RestController
@RequestMapping("/v2/characters")
@RequiredArgsConstructor
public class GameCharacterControllerV2 {

    private final GameCharacterService gameCharacterService;

    /**
     * 캐릭터 생성 (인증된 사용자)
     * memberId를 Authentication에서 자동 추출
     */
    @Operation(summary = "캐릭터 생성", description = "인증된 사용자의 새로운 캐릭터를 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "캐릭터 생성 성공",
                    content = @Content(schema = @Schema(implementation = GameCharacterResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (이미 캐릭터가 존재하거나 잘못된 종족)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping
    public RsData<GameCharacterResponse> createCharacter(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CreateCharacterRequestV2 request) {
        
        if (userDetails == null) {
            return RsData.of("401", "인증이 필요합니다.", null);
        }
        
        String memberId = userDetails.getId();
        log.info("캐릭터 생성 요청 - 사용자: {}, 종족: {}", memberId, request.getRace());
        
        // 기존 서비스 재사용을 위한 변환
        var createRequest = request.toCreateCharacterRequest(memberId);
        GameCharacterResponse response = gameCharacterService.createCharacter(createRequest);
        
        return RsData.of("201", "캐릭터 생성 완료", response);
    }

    /**
     * 내 캐릭터 조회 (인증된 사용자)
     */
    @Operation(summary = "내 캐릭터 조회", description = "인증된 사용자의 캐릭터 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GameCharacterDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "캐릭터 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/my")
    public RsData<GameCharacterDetailResponse> getMyCharacter(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            return RsData.of("401", "인증이 필요합니다.", null);
        }
        
        String memberId = userDetails.getId();
        log.info("내 캐릭터 조회 - 사용자: {}", memberId);
        
        try {
            GameCharacterResponse character = gameCharacterService.findByMemberId(memberId);
            GameCharacterDetailResponse response = gameCharacterService.findDetailById(character.id());
            return RsData.of("200", "조회 성공", response);
        } catch (IllegalArgumentException e) {
            return RsData.of("404", "캐릭터가 존재하지 않습니다.", null);
        }
    }

    /**
     * 내 캐릭터 기본 정보 조회
     */
    @Operation(summary = "내 캐릭터 기본 정보 조회", description = "인증된 사용자의 캐릭터 기본 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GameCharacterResponse.class))),
            @ApiResponse(responseCode = "404", description = "캐릭터 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/my/basic")
    public RsData<GameCharacterResponse> getMyCharacterBasic(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            return RsData.of("401", "인증이 핔요합니다.", null);
        }
        
        String memberId = userDetails.getId();
        log.info("내 캐릭터 기본 정보 조회 - 사용자: {}", memberId);
        
        try {
            GameCharacterResponse response = gameCharacterService.findByMemberId(memberId);
            return RsData.of("200", "조회 성공", response);
        } catch (IllegalArgumentException e) {
            return RsData.of("404", "캐릭터가 존재하지 않습니다.", null);
        }
    }

    /**
     * 내 캐릭터 경험치 추가
     */
    @Operation(summary = "경험치 추가", description = "인증된 사용자의 캐릭터에 경험치를 추가합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "경험치 추가 성공",
                    content = @Content(schema = @Schema(implementation = GameCharacterResponse.class))),
            @ApiResponse(responseCode = "404", description = "캐릭터 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping("/my/experience")
    public RsData<GameCharacterResponse> addExperience(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody AddExperienceRequest request) {
        
        if (userDetails == null) {
            return RsData.of("401", "인증이 필요합니다.", null);
        }
        
        String memberId = userDetails.getId();
        log.info("경험치 추가 - 사용자: {}, 경험치: {}", memberId, request.experience());
        
        try {
            // memberId로 캐릭터 찾기
            GameCharacterResponse character = gameCharacterService.findByMemberId(memberId);
            GameCharacterResponse response = gameCharacterService.addExperience(character.id(), request.experience());
            return RsData.of("200", "경험치 추가 완료", response);
        } catch (IllegalArgumentException e) {
            return RsData.of("404", "캐릭터가 존재하지 않습니다.", null);
        }
    }


}