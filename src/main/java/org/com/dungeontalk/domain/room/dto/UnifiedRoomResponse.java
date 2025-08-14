package org.com.dungeontalk.domain.room.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.com.dungeontalk.domain.room.common.RoomType;
import org.com.dungeontalk.domain.room.common.UnifiedRoomStatus;
import org.com.dungeontalk.domain.aichat.dto.response.AiGameRoomResponse;
import org.com.dungeontalk.domain.aichat.common.AiGameStatus;
import org.com.dungeontalk.domain.chat.dto.ChatRoomDto;

import java.time.Instant;
import java.util.List;

@Schema(description = "통합 룸 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnifiedRoomResponse {

    @Schema(description = "룸 ID", example = "room-12345")
    private String roomId;
    
    @Schema(description = "룸 타입", example = "AI_GAME")
    private RoomType roomType;
    
    @Schema(description = "룸 이름", example = "재미있는 던전탐험")
    private String roomName;
    
    @Schema(description = "룸 설명", example = "초보자도 환영하는 던전탐험")
    private String description;
    
    @Schema(description = "룸 상태", example = "CREATED")
    private UnifiedRoomStatus status;
    
    @Schema(description = "현재 참여자 수", example = "2")
    private int currentParticipants;
    
    @Schema(description = "최대 참여자 수", example = "4")
    private int maxParticipants;
    
    @Schema(description = "참여자 ID 목록")
    private List<String> participantIds;
    
    @Schema(description = "생성 시간 (UTC)")
    private Instant createdAt;
    
    @Schema(description = "수정 시간 (UTC)")
    private Instant updatedAt;

    @Schema(description = "연결된 게임 ID (AI 게임룸용)", example = "game-12345")
    private String gameId;
    
    @Schema(description = "현재 턴 번호 (AI 게임룸용)", example = "3")
    private Integer currentTurn;
    
    @Schema(description = "게임 설정 (AI 게임룸용)", example = "{\"difficulty\": \"normal\"}")
    private String gameSettings;
    
    @Schema(description = "마지막 활동 시간 (AI 게임룸용)")
    private Instant lastActivity;

    @Schema(description = "최대 용량 (플레이어 채팅룸용)", example = "10")
    private Long maxCapacity;
    
    @Schema(description = "현재 온라인 사용자 수 (플레이어 채팅룸용)", example = "5")
    private Long onlineUserCount;

    /**
     * AI 게임룸 응답인지 확인
     */
    public boolean isAiGameRoom() {
        return roomType == RoomType.AI_GAME;
    }

    /**
     * 플레이어 채팅룸 응답인지 확인
     */
    public boolean isPlayerChatRoom() {
        return roomType == RoomType.PLAYER_CHAT;
    }

    /**
     * 룸이 입장 가능한지 확인
     */
    public boolean isJoinable() {
        return status != null && status.isUsable() && 
               currentParticipants < maxParticipants;
    }

    /**
     * AI 게임룸 응답으로부터 통합 응답 생성
     */
    public static UnifiedRoomResponse fromAiGameRoom(AiGameRoomResponse aiResponse) {
        return UnifiedRoomResponse.builder()
                .roomId(aiResponse.getId())
                .roomType(RoomType.AI_GAME)
                .roomName(aiResponse.getRoomName())
                .status(mapAiGameStatus(aiResponse.getStatus()))
                .currentParticipants(aiResponse.getCurrentParticipantCount())
                .maxParticipants(aiResponse.getMaxParticipants())
                .participantIds(aiResponse.getParticipants())
                .createdAt(aiResponse.getCreatedAt() != null ? 
                          aiResponse.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toInstant() : null)
                // AI 게임룸 전용 필드
                .gameId(aiResponse.getGameId())
                .currentTurn(aiResponse.getCurrentTurn())
                .build();
    }

    /**
     * 플레이어 채팅룸 응답으로부터 통합 응답 생성
     */
    public static UnifiedRoomResponse fromPlayerChatRoom(ChatRoomDto chatResponse) {
        return UnifiedRoomResponse.builder()
                .roomId(chatResponse.getId())
                .roomType(RoomType.PLAYER_CHAT)
                .roomName(chatResponse.getRoomName())
                .status(UnifiedRoomStatus.CHAT_AVAILABLE) // 기본 상태
                .createdAt(chatResponse.getCreatedAt())
                .updatedAt(chatResponse.getUpdatedAt())
                .build();
    }

    /**
     * AI 게임 상태를 통합 상태로 매핑
     */
    private static UnifiedRoomStatus mapAiGameStatus(AiGameStatus aiStatus) {
        if (aiStatus == null) {
            return UnifiedRoomStatus.CREATED;
        }
        
        switch (aiStatus) {
            case CREATED:
                return UnifiedRoomStatus.CREATED;
            case ACTIVE:
                return UnifiedRoomStatus.ACTIVE;
            case PAUSED:
                return UnifiedRoomStatus.PAUSED;
            case COMPLETED:
                return UnifiedRoomStatus.COMPLETED;
            case ERROR:
                return UnifiedRoomStatus.ERROR;
            default:
                return UnifiedRoomStatus.CREATED;
        }
    }
}