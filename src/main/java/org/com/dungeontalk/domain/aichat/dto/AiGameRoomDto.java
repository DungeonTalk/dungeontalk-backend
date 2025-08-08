package org.com.dungeontalk.domain.aichat.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.com.dungeontalk.domain.aichat.common.AiGamePhase;
import org.com.dungeontalk.domain.aichat.common.AiGameStatus;
import org.com.dungeontalk.domain.aichat.entity.AiGameRoom;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class AiGameRoomDto {

    private String id;
    private String gameId;
    private String roomName;
    private String description;
    private AiGameStatus status;
    private AiGamePhase currentPhase;
    private int currentTurn;
    private int maxParticipants;
    private List<String> participants;
    private String gameSettings;
    private LocalDateTime lastActivity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity를 DTO로 변환
     */
    public static AiGameRoomDto fromEntity(AiGameRoom room) {
        return AiGameRoomDto.builder()
                .id(room.getId())
                .gameId(room.getGameId())
                .roomName(room.getRoomName())
                .description(room.getDescription())
                .status(room.getStatus())
                .currentPhase(room.getCurrentPhase())
                .currentTurn(room.getCurrentTurn())
                .maxParticipants(room.getMaxParticipants())
                .participants(room.getParticipants())
                .gameSettings(room.getGameSettings())
                .lastActivity(room.getLastActivity())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    /**
     * 현재 참여 인원 수
     */
    public int getCurrentParticipantCount() {
        return this.participants != null ? this.participants.size() : 0;
    }

    /**
     * 입장 가능 여부
     */
    public boolean canJoin() {
        return this.status == AiGameStatus.CREATED && 
               getCurrentParticipantCount() < this.maxParticipants;
    }

    /**
     * 활성 상태 여부
     */
    public boolean isActive() {
        return this.status == AiGameStatus.ACTIVE;
    }
}