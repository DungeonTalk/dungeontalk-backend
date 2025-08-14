package org.com.dungeontalk.domain.gamecharacter.dto.response;

import org.com.dungeontalk.domain.gamecharacter.entity.GameCharacter;

import java.time.Instant;
import java.util.Map;

public record GameCharacterDetailResponse(
        String id,
        String memberId,
        String raceTypeId,
        String raceName,
        Integer playerLevel,
        Long totalExp,
        Integer unspentPoints,
        Integer str,
        Integer wil,
        Integer int_,
        Integer wis,
        Integer dex,
        Integer lux,
        Double hp,
        Double mp,
        Double physicalAttack,
        Double magicAttack,
        Double evasionRate,
        Double accuracy,
        Double diceOdds,
        Instant createdAt,
        Instant updatedAt
) {
    public static GameCharacterDetailResponse from(GameCharacter character, String raceName, Map<String, Double> calculatedStats) {
        return new GameCharacterDetailResponse(
                character.getId(),
                character.getMemberId(),
                character.getRaceTypeId(),
                raceName,
                character.getPlayerLevel(),
                character.getTotalExp(),
                character.getUnspentPoints(),
                character.getStr(),
                character.getWil(),
                character.getInt_(),
                character.getWis(),
                character.getDex(),
                character.getLux(),
                calculatedStats.getOrDefault("hp", 0.0),
                calculatedStats.getOrDefault("mp", 0.0),
                calculatedStats.getOrDefault("physicalAttack", 0.0),
                calculatedStats.getOrDefault("magicAttack", 0.0),
                calculatedStats.getOrDefault("evasionRate", 0.0),
                calculatedStats.getOrDefault("accuracy", 0.0),
                calculatedStats.getOrDefault("diceOdds", 0.0),
                character.getCreatedAt(),
                character.getUpdatedAt()
        );
    }
}