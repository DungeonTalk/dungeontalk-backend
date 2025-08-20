package org.com.dungeontalk.domain.gamecharacter.service;

import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.gamecharacter.dto.request.CreateCharacterRequest;
import org.com.dungeontalk.domain.gamecharacter.dto.request.GameResultRequest;
import org.com.dungeontalk.domain.gamecharacter.dto.response.GameCharacterDetailResponse;
import org.com.dungeontalk.domain.gamecharacter.dto.response.GameCharacterResponse;
import org.com.dungeontalk.domain.gamecharacter.entity.GameCharacter;
import org.com.dungeontalk.domain.gamecharacter.entity.RequestExp;
import org.com.dungeontalk.domain.gamecharacter.repository.GameCharacterRepository;
import org.com.dungeontalk.domain.gamecharacter.repository.RequestExpRepository;
import org.com.dungeontalk.domain.stat.repository.RaceStatsRepository;
import org.com.dungeontalk.domain.stat.service.StatAggregateService;
import org.com.dungeontalk.domain.world.entity.World;
import org.com.dungeontalk.domain.world.repository.WorldRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import org.com.dungeontalk.domain.stat.entity.RaceStats;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameCharacterService {

    private final GameCharacterRepository gameCharacterRepository;
    private final RaceStatsRepository raceStatsRepository;
    private final StatAggregateService statAggregateService;
    private final RequestExpRepository requestExpRepository;
    private final WorldRepository worldRepository;

    // 새로운 캐릭터 생성 (레벨 1, 모든 스탯 10으로 초기화)
    @Transactional
    public GameCharacterResponse createCharacter(CreateCharacterRequest request) {
        // UUID로 RaceStats 조회
        RaceStats raceStats = raceStatsRepository.findById(request.raceId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 종족: " + request.raceId()));

        GameCharacter character = new GameCharacter();
        character.setMemberId(request.memberId());
        character.setRaceId(raceStats.getId()); // UUID 저장
        character.setPlayerLevel(1);
        character.setTotalExp(0L);
        character.setUnspentPoints(0);

        // 모든 스탯을 10으로 고정
        character.setStrength(10);
        character.setWillpower(10);
        character.setIntelligence(10);
        character.setWisdom(10);
        character.setDexterity(10);
        character.setLuck(10);

        GameCharacter savedCharacter = gameCharacterRepository.save(character);
        return GameCharacterResponse.from(savedCharacter);
    }

    // 캐릭터 ID로 기본 정보 조회 (종족 스탯 없이)
    public GameCharacterResponse findById(String id) {
        GameCharacter character = gameCharacterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + id));
        return GameCharacterResponse.from(character);
    }

    // 캐릭터 ID로 조회 + 종족 스탯 정보 포함 (fetch join 사용), 프론트에 데이터 보낼 때 사용
    public GameCharacterResponse findByIdWithRace(String id) {
        GameCharacter character = gameCharacterRepository.findWithRace(id)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + id));
        return GameCharacterResponse.from(character);
    }

    // 멤버 ID로 해당 멤버의 캐릭터 조회 (MVP: 1개 멤버당 1개 캐릭터)
    public GameCharacterResponse findByMemberId(String memberId) {
        GameCharacter character = gameCharacterRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found for member: " + memberId));
        return GameCharacterResponse.from(character);
    }

    // 캐릭터 상세 정보 조회 (기본 정보 + 종족명 + 계산된 스탯)
    public GameCharacterDetailResponse findDetailById(String id) {
        GameCharacter character = gameCharacterRepository.findWithRace(id)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + id));

        // 종족명 가져오기
        String raceName = character.getRaceStats().getRace();

        // 모든 스탯 계산
        Map<String, Double> calculatedStats = statAggregateService.calculateAllStats(id);

        return GameCharacterDetailResponse.from(character.getMember().getNickName(), character, raceName, calculatedStats); // (수정) 닉네임 추가
    }

    // 사용 가능한 모든 종족 목록 조회
    public List<String> getRaces() {
        return raceStatsRepository.findAllRaceNames();
    }

    // 멤버가 캐릭터를 가지고 있는지 확인
    public boolean hasCharacter(String memberId) {
        return gameCharacterRepository.existsByMemberId(memberId);
    }

    @Transactional
    public GameCharacterResponse addExperience(String characterId, int expToAdd) {
        // 캐릭터 정보 조회
        GameCharacter character = gameCharacterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("캐릭터 정보를 찾을 수 없습니다: " + characterId));

        // 총 경험치 업데이트
        character.setTotalExp(character.getTotalExp() + expToAdd);

        // 레벨업 처리 (반복문을 사용해 여러 레벨업도 한번에 처리)
        while (true) {
            int currentLevel = character.getPlayerLevel();

            // 현재 레벨의 필요 경험치 정보 조회
            RequestExp currentLevelInfo = requestExpRepository.findByLevel(currentLevel)
                    .orElseThrow(() -> new IllegalStateException("레벨 정보를 찾을 수 없습니다: " + currentLevel));

            // 만렙인지 확인(getRequestNextLevelExp -> 만렙(30)인 경우 해당 컬럼이 유일하게 0)
            if (currentLevelInfo.getRequestNextLevelExp() == 0) {
                break; // 만렙이면 더 이상 레벨업하지 않음
            }

            // 다음 레벨업에 필요한 총 경험치량 (=현재 레벨의 총 요구 경험치 + 다음 레벨 필요 경험치)
            long requiredTotalExpForNextLevel = currentLevelInfo.getRequestTotalExp() + currentLevelInfo.getRequestNextLevelExp();

            // 레벨업 조건 확인
            if (character.getTotalExp() >= requiredTotalExpForNextLevel) {
                // 레벨업!
                character.setPlayerLevel(currentLevel + 1);
            } else {
                // 경험치가 부족하면 레벨업 중단
                break;
            }
        }

        // 변경된 캐릭터 정보 저장 및 반환
        GameCharacter updatedCharacter = gameCharacterRepository.save(character);
        return GameCharacterResponse.from(updatedCharacter);
    }


    @Transactional
    public GameCharacterResponse processGameResult(GameResultRequest request) {
        int expToAdd = 0;

        // 클리어확인 여부 -> 1 이면 클리어
        if (request.isCleared() == 1) {
            World world = worldRepository.findById(request.worldId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 월드: " + request.worldId()));
            expToAdd = world.getClearExp();
        }

        return addExperience(request.characterId(), expToAdd);
    }

}