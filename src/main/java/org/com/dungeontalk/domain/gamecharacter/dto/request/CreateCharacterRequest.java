package org.com.dungeontalk.domain.gamecharacter.dto.request;

public record CreateCharacterRequest(
        String memberId,
        String raceTypeId
) {
}