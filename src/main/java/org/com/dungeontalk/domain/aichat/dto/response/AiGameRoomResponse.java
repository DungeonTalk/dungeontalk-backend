package org.com.dungeontalk.domain.aichat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.com.dungeontalk.domain.aichat.common.AiGamePhase;
import org.com.dungeontalk.domain.aichat.common.AiGameStatus;
import org.com.dungeontalk.domain.aichat.entity.AiGameRoom;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Schema(description = "AI 게임방 응답")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiGameRoomResponse {

    @Schema(description = "게임방 ID", example = "room-12345")
    private String id;
    
    @Schema(description = "게임방 ID (프론트엔드 호환성)", example = "room-12345")
    private String roomId; // 프론트엔드 호환성을 위한 필드 (id와 동일한 값)
    
    @Schema(description = "게임 ID", example = "game-12345")
    private String gameId;
    
    @Schema(description = "게임방 이름", example = "재미있는 던전탐험")
    private String roomName;
    
    @Schema(description = "게임 상태", example = "CREATED")
    private AiGameStatus status;
    
    @Schema(description = "현재 게임 단계", example = "TURN_INPUT")
    private AiGamePhase currentPhase;
    
    @Schema(description = "현재 턴 수", example = "1")
    private int currentTurn;
    
    @Schema(description = "최대 참가자 수", example = "4")
    private int maxParticipants;
    
    @Schema(description = "현재 참가자 수", example = "2")
    private int currentParticipantCount;
    
    @Schema(description = "참가자 목록")
    private List<String> participants;
    
    @Schema(description = "생성 시간")
    private Instant createdAt;

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
                .status(room.getStatus())
                .currentPhase(room.getCurrentPhase())
                .currentTurn(room.getCurrentTurn())
                .maxParticipants(room.getMaxParticipants())
                .currentParticipantCount(room.getCurrentParticipantCount())
                .participants(Optional.ofNullable(room.getParticipants())
                        .map(ArrayList::new)
                        .orElseGet(ArrayList::new))
                .createdAt(room.getCreatedAt())
                .build();
    }
}