package org.com.dungeontalk.domain.gamecharacter.dto.response;

import org.com.dungeontalk.domain.gamecharacter.entity.GameCharacter;

import java.time.Instant;

public record GameCharacterResponse(
        String id,
        String memberId,
        String raceId,
        Integer playerLevel,
        Long totalExp,
        Integer unspentPoints,
        Integer strength,
        Integer willpower,
        Integer intelligence,
        Integer wisdom,
        Integer dexterity,
        Integer luck,
        Instant createdAt,
        Instant updatedAt
) {
    public static GameCharacterResponse from(GameCharacter character) {
        return new GameCharacterResponse(
                character.getId(),
                character.getMemberId(),
                character.getRaceId(),
                character.getPlayerLevel(),
                character.getTotalExp(),
                character.getUnspentPoints(),
                character.getStrength(),
                character.getWillpower(),
                character.getIntelligence(),
                character.getWisdom(),
                character.getDexterity(),
                character.getLuck(),
                character.getCreatedAt(),
                character.getUpdatedAt()
        );
    }
}