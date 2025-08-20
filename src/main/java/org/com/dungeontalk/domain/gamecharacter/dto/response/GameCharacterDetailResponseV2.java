package org.com.dungeontalk.domain.gamecharacter.dto.response;

import org.com.dungeontalk.domain.gamecharacter.entity.GameCharacter;

import java.time.Instant;
import java.util.Map;

/**
 * V2 게임 캐릭터 상세 응답 DTO
 * 보안을 위해 memberId를 제외한 버전
 */
public record GameCharacterDetailResponseV2(
        String id,
        String nickname,
        String raceId,
        String raceName,
        Integer playerLevel,
        Long totalExp,
        Integer unspentPoints,
        Integer strength,
        Integer willpower,
        Integer intelligence,
        Integer wisdom,
        Integer dexterity,
        Integer luck,
        Double healthPoints,
        Double manaPoints,
        Double physicalAttack,
        Double magicAttack,
        Double evasionRate,
        Double accuracy,
        Double diceOdds,
        Instant createdAt,
        Instant updatedAt
) {
    public static GameCharacterDetailResponseV2 from(String nickname, GameCharacter character, String raceName, Map<String, Double> calculatedStats) {
        return new GameCharacterDetailResponseV2(
                character.getId(),
                nickname,
                character.getRaceId(),
                raceName,
                character.getPlayerLevel(),
                character.getTotalExp(),
                character.getUnspentPoints(),
                character.getStrength(),
                character.getWillpower(),
                character.getIntelligence(),
                character.getWisdom(),
                character.getDexterity(),
                character.getLuck(),
                calculatedStats.getOrDefault("healthPoints", 0.0),
                calculatedStats.getOrDefault("manaPoints", 0.0),
                calculatedStats.getOrDefault("physicalAttack", 0.0),
                calculatedStats.getOrDefault("magicAttack", 0.0),
                calculatedStats.getOrDefault("evasionRate", 0.0),
                calculatedStats.getOrDefault("accuracy", 0.0),
                calculatedStats.getOrDefault("diceOdds", 0.0),
                character.getCreatedAt(),
                character.getUpdatedAt()
        );
    }
    
    /**
     * 기존 GameCharacterDetailResponse를 V2로 변환
     */
    public static GameCharacterDetailResponseV2 from(GameCharacterDetailResponse response) {
        return new GameCharacterDetailResponseV2(
                response.id(),
                response.nickname(),
                response.raceId(),
                response.raceName(),
                response.playerLevel(),
                response.totalExp(),
                response.unspentPoints(),
                response.strength(),
                response.willpower(),
                response.intelligence(),
                response.wisdom(),
                response.dexterity(),
                response.luck(),
                response.healthPoints(),
                response.manaPoints(),
                response.physicalAttack(),
                response.magicAttack(),
                response.evasionRate(),
                response.accuracy(),
                response.diceOdds(),
                response.createdAt(),
                response.updatedAt()
        );
    }
}