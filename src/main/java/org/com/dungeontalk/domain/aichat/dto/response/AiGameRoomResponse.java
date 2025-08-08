package org.com.dungeontalk.domain.aichat.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.com.dungeontalk.domain.aichat.common.AiGamePhase;
import org.com.dungeontalk.domain.aichat.common.AiGameStatus;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class AiGameRoomResponse {

    private String id;
    private String gameId;
    private String roomName;
    private String description;
    private AiGameStatus status;
    private AiGamePhase currentPhase;
    private int currentTurn;
    private int maxParticipants;
    private int currentParticipantCount;
    private List<String> participants;
    private LocalDateTime lastActivity;
    private LocalDateTime createdAt;

    /**
     * 입장 가능 여부
     */
    public boolean canJoin() {
        return this.status == AiGameStatus.CREATED && 
               this.currentParticipantCount < this.maxParticipants;
    }

    /**
     * 게임 진행 중 여부
     */
    public boolean isActive() {
        return this.status == AiGameStatus.ACTIVE;
    }
}