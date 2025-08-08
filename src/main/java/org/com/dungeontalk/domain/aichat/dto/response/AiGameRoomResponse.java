package org.com.dungeontalk.domain.aichat.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.com.dungeontalk.domain.aichat.common.AiGamePhase;
import org.com.dungeontalk.domain.aichat.common.AiGameStatus;
import org.com.dungeontalk.domain.aichat.entity.AiGameRoom;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@Builder
public class AiGameRoomResponse {

    private String id;
    private String roomId; // 프론트엔드 호환성을 위한 필드 (id와 동일한 값)
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

    /**
     * Entity에서 Response DTO로 변환
     */
    public static AiGameRoomResponse fromEntity(AiGameRoom room) {
        return AiGameRoomResponse.builder()
                .id(room.getId())
                .roomId(room.getId()) // 프론트엔드 호환성을 위해 동일한 값 설정
                .gameId(room.getGameId())
                .roomName(room.getRoomName())
                .description(room.getDescription())
                .status(room.getStatus())
                .currentPhase(room.getCurrentPhase())
                .currentTurn(room.getCurrentTurn())
                .maxParticipants(room.getMaxParticipants())
                .currentParticipantCount(room.getCurrentParticipantCount())
                .participants(Optional.ofNullable(room.getParticipants())
                        .map(ArrayList::new)
                        .orElseGet(ArrayList::new))
                .lastActivity(room.getLastActivity())
                .createdAt(room.getCreatedAt())
                .build();
    }
}