package org.com.dungeontalk.domain.gamecharacter.dto.request;

import jakarta.validation.constraints.NotNull;

public record GameResultRequest(
        @NotNull String characterId,
        @NotNull Long worldId,
        @NotNull int isCleared
) {
}
