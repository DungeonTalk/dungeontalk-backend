package org.com.dungeontalk.domain.member.dto.response;

import org.com.dungeontalk.domain.gamecharacter.dto.response.GameCharacterResponse;

public record RegisterResponse(
        String id,
        String name,
        String nickName,
        GameCharacterResponse character
) {
}
