package org.com.dungeontalk.domain.gamecharacter.controller;

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
    @PostMapping
    public RsData<GameCharacterResponse> createCharacter(@RequestBody CreateCharacterRequest request) {
        var character = gameCharacterService.createCharacter(request);
        var response = GameCharacterResponse.from(character);
        return RsData.of("201", "캐릭터 생성 완료", response);
    }

    // 특정 캐릭터 기본 정보 조회 (계산된 스탯 없이)
    @GetMapping("/basic/{id}")
    public RsData<GameCharacterResponse> getCharacterBasic(@PathVariable String id) {
        var character = gameCharacterService.findById(id);
        var response = GameCharacterResponse.from(character);
        return RsData.of("200", "캐릭터 조회 완료", response);
    }

    // 특정 캐릭터 상세 정보 조회 (계산된 스탯 포함)
    @GetMapping("/{id}")
    public RsData<GameCharacterDetailResponse> getCharacterDetail(@PathVariable String id) {
        var response = gameCharacterService.findDetailById(id);
        return RsData.of("200", "캐릭터 상세 조회 완료", response);
    }

    // 특정 멤버의 캐릭터 조회 (MVP: 1개 멤버당 1개 캐릭터)
    @GetMapping
    public RsData<GameCharacterResponse> getCharacterByMember(@RequestParam String memberId) {
        var character = gameCharacterService.findByMemberId(memberId);
        var response = GameCharacterResponse.from(character);
        return RsData.of("200", "멤버 캐릭터 조회 완료", response);
    }

    // 사용 가능한 모든 종족 목록 조회 (캐릭터 생성 시 종족 선택에서 사용)
    @GetMapping("/races")
    public RsData<List<String>> getRaces() {
        var races = raceStatsRepository.findAllRaceNames();
        return RsData.of("200", "종족 목록 조회 완료", races);
    }
}