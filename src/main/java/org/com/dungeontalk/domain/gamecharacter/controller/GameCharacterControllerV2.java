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
import org.com.dungeontalk.domain.gamecharacter.dto.response.GameCharacterDetailResponseV2;
import org.com.dungeontalk.domain.gamecharacter.dto.response.GameCharacterResponse;
import org.com.dungeontalk.domain.gamecharacter.dto.response.GameCharacterResponseV2;
import org.com.dungeontalk.domain.gamecharacter.service.GameCharacterService;
import org.com.dungeontalk.global.rsData.RsData;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.com.dungeontalk.global.security.CustomUserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * 게임 캐릭터 컨트롤러 V2
 * - 인증된 사용자의 memberId를 자동으로 추출
 * - Spring Security를 통한 인증 처리
 */
@Slf4j
@Tag(name = "게임 캐릭터 V2", description = "게임 캐릭터 정보 조회 관련 API V2 (인증 기반)")
@Controller
@RequestMapping({"/v2/characters", "/game/character"})
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
                    content = @Content(schema = @Schema(implementation = GameCharacterResponseV2.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (이미 캐릭터가 존재하거나 잘못된 종족)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping(consumes = "application/json", produces = "application/json")
    @ResponseBody
    public RsData<GameCharacterResponseV2> createCharacter(
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
        
        // V2 DTO로 변환하여 반환 (memberId 제외)
        return RsData.of("201", "캐릭터 생성 완료", GameCharacterResponseV2.from(response));
    }

    /**
     * 내 캐릭터 조회 (인증된 사용자)
     */
    @Operation(summary = "내 캐릭터 조회", description = "인증된 사용자의 캐릭터 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GameCharacterDetailResponseV2.class))),
            @ApiResponse(responseCode = "404", description = "캐릭터 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/my")
    @ResponseBody
    public RsData<GameCharacterDetailResponseV2> getMyCharacter(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            return RsData.of("401", "인증이 필요합니다.", null);
        }
        
        String memberId = userDetails.getId();
        log.info("내 캐릭터 조회 - 사용자: {}", memberId);
        
        try {
            GameCharacterResponse character = gameCharacterService.findByMemberId(memberId);
            // N+1 문제가 해결된 V2 메서드 사용
            GameCharacterDetailResponseV2 response = gameCharacterService.findDetailByIdV2(character.id());
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
                    content = @Content(schema = @Schema(implementation = GameCharacterResponseV2.class))),
            @ApiResponse(responseCode = "404", description = "캐릭터 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/my/basic")
    @ResponseBody
    public RsData<GameCharacterResponseV2> getMyCharacterBasic(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        if (userDetails == null) {
            return RsData.of("401", "인증이 필요합니다.", null);
        }
        
        String memberId = userDetails.getId();
        log.info("내 캐릭터 기본 정보 조회 - 사용자: {}", memberId);
        
        try {
            GameCharacterResponse response = gameCharacterService.findByMemberId(memberId);
            // V2 DTO로 변환하여 반환 (memberId 제외)
            return RsData.of("200", "조회 성공", GameCharacterResponseV2.from(response));
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
                    content = @Content(schema = @Schema(implementation = GameCharacterResponseV2.class))),
            @ApiResponse(responseCode = "404", description = "캐릭터 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping("/my/experience")
    @ResponseBody
    public RsData<GameCharacterResponseV2> addExperience(
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
            // V2 DTO로 변환하여 반환 (memberId 제외)
            return RsData.of("200", "경험치 추가 완료", GameCharacterResponseV2.from(response));
        } catch (IllegalArgumentException e) {
            return RsData.of("404", "캐릭터가 존재하지 않습니다.", null);
        }
    }

    /**
     * 캐릭터 정보 페이지 (HTMX 사용)
     */
    @GetMapping("/info")
    public String getCharacterInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails, 
            Model model) {

        
        String memberId = userDetails.getId();
        log.info("캐릭터 정보 페이지 - 사용자: {}", memberId);
        
        try {
            GameCharacterResponse character = gameCharacterService.findByMemberId(memberId);
            // N+1 문제가 해결된 V2 메서드 사용
            GameCharacterDetailResponseV2 characterDetail = gameCharacterService.findDetailByIdV2(character.id());
            model.addAttribute("character", characterDetail);
            return "fragments/character-htmx :: character-display";
        } catch (IllegalArgumentException e) {
            log.info("캐릭터 없음 - 사용자: {}", memberId);
            model.addAttribute("character", null);
            return "fragments/character-htmx :: character-display";
        }
    }

    /**
     * 캐릭터 생성 폼 (HTMX)
     */
    @GetMapping("/create-form")
    public String getCharacterCreateForm() {
        return "fragments/character-htmx :: character-create-form";
    }


}