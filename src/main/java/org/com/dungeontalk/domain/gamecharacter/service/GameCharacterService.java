package org.com.dungeontalk.domain.gamecharacter.service;

import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.gamecharacter.dto.request.CreateCharacterRequest;
import org.com.dungeontalk.domain.gamecharacter.dto.response.GameCharacterDetailResponse;
import org.com.dungeontalk.domain.gamecharacter.entity.GameCharacter;
import org.com.dungeontalk.domain.gamecharacter.repository.GameCharacterRepository;
import org.com.dungeontalk.domain.stat.repository.RaceStatsRepository;
import org.com.dungeontalk.domain.stat.service.StatAggregateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameCharacterService {

    private final GameCharacterRepository gameCharacterRepository;
    private final RaceStatsRepository raceStatsRepository;
    private final StatAggregateService statAggregateService;

    @Transactional
    public GameCharacter createCharacter(CreateCharacterRequest request) {
        // 종족명으로 RaceStats 조회하여 UUID 가져오기
        var raceStats = raceStatsRepository.findByRace(request.raceTypeId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 종족: " + request.raceTypeId()));

        GameCharacter character = new GameCharacter();
        character.setMemberId(request.memberId());
        character.setRaceTypeId(raceStats.getId()); // UUID 저장
        character.setPlayerLevel(1);
        character.setTotalExp(0L);
        character.setUnspentPoints(0);

        // 모든 스탯을 10으로 고정
        character.setStr(10);
        character.setWil(10);
        character.setInt_(10);
        character.setWis(10);
        character.setDex(10);
        character.setLux(10);

        return gameCharacterRepository.save(character);
    }

    public GameCharacter findById(String id) {
        return gameCharacterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + id));
    }

    public GameCharacter findByIdWithRace(String id) {
        return gameCharacterRepository.findWithRace(id)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + id));
    }

    public List<GameCharacter> findByMemberId(String memberId) {
        return gameCharacterRepository.findByMemberId(memberId);
    }

    public GameCharacterDetailResponse findDetailById(String id) {
        GameCharacter character = gameCharacterRepository.findWithRace(id)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + id));

        // 종족명 가져오기
        String raceName = character.getRaceStats().getRace();

        // 모든 스탯 계산
        var calculatedStats = statAggregateService.calculateAllStats(id);

        return GameCharacterDetailResponse.from(character, raceName, calculatedStats);
    }
}