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

    // 특정 캐릭터 상세 정보 조회 (캐릭터 상세 화면에서 사용)
    @GetMapping("/{id}")
    public GameCharacterDetailResponse getCharacter(@PathVariable String id) {
        return gameCharacterService.findDetailById(id);
    }

    // 특정 멤버가 소유한 모든 캐릭터 목록 조회 (캐릭터 선택 화면에서 사용)
    @GetMapping
    public List<GameCharacterResponse> getCharactersByMember(@RequestParam String memberId) {
        var characters = gameCharacterService.findByMemberId(memberId);
        return characters.stream()
                .map(GameCharacterResponse::from)
                .toList();
    }

    // 사용 가능한 모든 종족 목록 조회 (캐릭터 생성 시 종족 선택에서 사용)
    @GetMapping("/races")
    public List<String> getRaces() {
        return raceStatsRepository.findAllRaceNames();
    }
}