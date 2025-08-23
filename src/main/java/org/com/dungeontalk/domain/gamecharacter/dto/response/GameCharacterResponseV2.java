package org.com.dungeontalk.domain.gamecharacter.dto.response;

import org.com.dungeontalk.domain.gamecharacter.entity.GameCharacter;

import java.time.Instant;

/**
 * V2 게임 캐릭터 응답 DTO
 * 보안을 위해 memberId를 제외한 버전
 */
public record GameCharacterResponseV2(
        String id,
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
    public static GameCharacterResponseV2 from(GameCharacter character) {
        return new GameCharacterResponseV2(
                character.getId(),
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
    
    /**
     * 기존 GameCharacterResponse를 V2로 변환
     */
    public static GameCharacterResponseV2 from(GameCharacterResponse response) {
        return new GameCharacterResponseV2(
                response.id(),
                response.raceId(),
                response.playerLevel(),
                response.totalExp(),
                response.unspentPoints(),
                response.strength(),
                response.willpower(),
                response.intelligence(),
                response.wisdom(),
                response.dexterity(),
                response.luck(),
                response.createdAt(),
                response.updatedAt()
        );
    }
}