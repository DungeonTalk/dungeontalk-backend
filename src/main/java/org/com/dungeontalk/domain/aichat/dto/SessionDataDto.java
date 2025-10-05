package org.com.dungeontalk.domain.aichat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.com.dungeontalk.domain.aichat.common.AiGamePhase;
import org.com.dungeontalk.domain.aichat.common.AiGameStatus;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SessionDataDto {
    private String roomId;
    private String gameId;
    private AiGameStatus status;
    private AiGamePhase phase;
    private int turn;
}