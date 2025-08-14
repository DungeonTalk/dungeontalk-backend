package org.com.dungeontalk.domain.gamecharacter.controller;

import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.gamecharacter.dto.request.CreateCharacterRequest;
import org.com.dungeontalk.domain.gamecharacter.dto.response.GameCharacterDetailResponse;
import org.com.dungeontalk.domain.gamecharacter.dto.response.GameCharacterResponse;
import org.com.dungeontalk.domain.gamecharacter.service.GameCharacterService;
import org.com.dungeontalk.domain.stat.repository.RaceStatsRepository;
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
    @ResponseStatus(HttpStatus.CREATED)
    public GameCharacterResponse createCharacter(@RequestBody CreateCharacterRequest request) {
        var character = gameCharacterService.createCharacter(request);
        return GameCharacterResponse.from(character);
    }

    // 특정 캐릭터 기본 정보 조회 (계산된 스탯 없이)
    @GetMapping("/basic/{id}")
    public GameCharacterResponse getCharacterBasic(@PathVariable String id) {
        var character = gameCharacterService.findById(id);
        return GameCharacterResponse.from(character);
    }

    // 특정 캐릭터 상세 정보 조회 (계산된 스탯 포함)
    @GetMapping("/{id}")
    public GameCharacterDetailResponse getCharacterDetail(@PathVariable String id) {
        return gameCharacterService.findDetailById(id);
    }

    // 특정 멤버의 캐릭터 조회 (MVP: 1개 멤버당 1개 캐릭터)
    @GetMapping
    public GameCharacterResponse getCharacterByMember(@RequestParam String memberId) {
        var character = gameCharacterService.findByMemberId(memberId);
        return GameCharacterResponse.from(character);
    }

    // 사용 가능한 모든 종족 목록 조회 (캐릭터 생성 시 종족 선택에서 사용)
    @GetMapping("/races")
    public List<String> getRaces() {
        return raceStatsRepository.findAllRaceNames();
    }
}