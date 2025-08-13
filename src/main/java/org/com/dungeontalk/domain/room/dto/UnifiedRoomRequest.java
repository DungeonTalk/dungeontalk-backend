package org.com.dungeontalk.domain.room.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.com.dungeontalk.domain.room.common.RoomType;
import org.com.dungeontalk.domain.chat.common.ChatMode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

@Schema(description = "통합 룸 생성 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnifiedRoomRequest {

    @Schema(description = "룸 타입", example = "AI_GAME", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "룸 타입은 필수입니다")
    private RoomType roomType;
    
    @Schema(description = "룸 이름", example = "재미있는 던전탐험", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "룸 이름은 필수입니다")
    private String roomName;
    
    @Schema(description = "룸 설명", example = "초보자도 환영하는 던전탐험")
    private String description;
    
    @Schema(description = "최대 참여자 수", example = "4")
    @Positive(message = "최대 참여자 수는 양수여야 합니다")
    private Integer maxParticipants;
    
    @Schema(description = "생성자 ID", example = "user-12345", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "생성자 ID는 필수입니다")
    private String creatorId;
    
    @Schema(description = "초기 참여자 ID 목록")
    private List<String> participantIds;

    @Schema(description = "연결된 게임 ID (AI 게임룸용)", example = "game-12345")
    private String gameId;
    
    @Schema(description = "게임 설정 JSON (AI 게임룸용)", example = "{\"difficulty\": \"normal\"}")
    private String gameSettings;

    @Schema(description = "채팅 모드 (플레이어 채팅룸용)", example = "MULTI")
    private ChatMode chatMode;
    
    @Schema(description = "최대 용량 (플레이어 채팅룸용)", example = "10")
    private Integer maxCapacity;

    /**
     * AI 게임룸 생성 요청인지 확인
     */
    public boolean isAiGameRoom() {
        return roomType == RoomType.AI_GAME;
    }

    /**
     * 플레이어 채팅룸 생성 요청인지 확인
     */
    public boolean isPlayerChatRoom() {
        return roomType == RoomType.PLAYER_CHAT;
    }

    /**
     * 유효성 검증 및 기본값 설정을 위한 빌더 생성
     */
    public static UnifiedRoomRequestBuilder aiGameRoom() {
        return UnifiedRoomRequest.builder()
                .roomType(RoomType.AI_GAME)
                .maxParticipants(3);
    }

    /**
     * 플레이어 채팅룸 생성을 위한 빌더
     */
    public static UnifiedRoomRequestBuilder playerChatRoom() {
        return UnifiedRoomRequest.builder()
                .roomType(RoomType.PLAYER_CHAT)
                .chatMode(ChatMode.MULTI);
    }

    /**
     * 룸 타입에 따른 유효성 검증
     */
    public void validateByRoomType() {
        if (roomType == null) {
            throw new IllegalArgumentException("룸 타입은 필수입니다");
        }
        
        if (isAiGameRoom() && maxParticipants == null) {
            throw new IllegalArgumentException("AI 게임룸은 최대 참여자 수가 필요합니다");
        }
        
        if (isPlayerChatRoom() && chatMode == null) {
            throw new IllegalArgumentException("플레이어 채팅룸은 채팅 모드가 필요합니다");
        }
    }
}