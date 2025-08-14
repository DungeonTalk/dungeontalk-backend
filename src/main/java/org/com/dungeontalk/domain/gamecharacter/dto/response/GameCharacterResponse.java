package org.com.dungeontalk.domain.gamecharacter.dto.response;

import org.com.dungeontalk.domain.gamecharacter.entity.GameCharacter;

import java.time.Instant;

public record GameCharacterResponse(
        String id,
        String memberId,
        String raceTypeId,
        Integer playerLevel,
        Long totalExp,
        Integer unspentPoints,
        Integer str,
        Integer wil,
        Integer int_,
        Integer wis,
        Integer dex,
        Integer luk,
        Instant createdAt,
        Instant updatedAt
) {
    public static GameCharacterResponse from(GameCharacter character) {
        return new GameCharacterResponse(
                character.getId(),
                character.getMemberId(),
                character.getRaceTypeId(),
                character.getPlayerLevel(),
                character.getTotalExp(),
                character.getUnspentPoints(),
                character.getStr(),
                character.getWil(),
                character.getInt_(),
                character.getWis(),
                character.getDex(),
                character.getLuk(),
                character.getCreatedAt(),
                character.getUpdatedAt()
        );
    }
}