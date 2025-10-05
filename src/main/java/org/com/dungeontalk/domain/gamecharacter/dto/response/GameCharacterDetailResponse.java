package org.com.dungeontalk.domain.gamecharacter.dto.response;

import org.com.dungeontalk.domain.gamecharacter.entity.GameCharacter;

import java.time.Instant;
import java.util.Map;

public record GameCharacterDetailResponse(
        String id,
        String memberId,
        String nickname, // (추가) 닉네임을 위한 필드
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
    public static GameCharacterDetailResponse from(String nickname, GameCharacter character, String raceName, Map<String, Double> calculatedStats) {
        return new GameCharacterDetailResponse(
                character.getId(),
                character.getMemberId(),
                nickname, // (수정) Member 엔티티의 닉네임 사용
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
}