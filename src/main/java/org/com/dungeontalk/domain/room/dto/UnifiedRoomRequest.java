package org.com.dungeontalk.domain.room.dto;

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

/**
 * 통합된 룸 생성 요청 DTO
 * AI 게임룸과 플레이어 채팅룸 생성 요청을 통합
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnifiedRoomRequest {

    // === 공통 필드 ===
    
    /**
     * 룸 타입 (필수)
     */
    @NotNull(message = "룸 타입은 필수입니다")
    private RoomType roomType;
    
    /**
     * 룸 이름 (필수)
     */
    @NotBlank(message = "룸 이름은 필수입니다")
    private String roomName;
    
    /**
     * 룸 설명 (선택)
     */
    private String description;
    
    /**
     * 최대 참여자 수 (선택, 기본값은 각 타입별 기본값 사용)
     */
    @Positive(message = "최대 참여자 수는 양수여야 합니다")
    private Integer maxParticipants;
    
    /**
     * 생성자 ID (필수)
     */
    @NotBlank(message = "생성자 ID는 필수입니다")
    private String creatorId;
    
    /**
     * 초기 참여자 ID 목록 (선택)
     */
    private List<String> participantIds;

    // === AI 게임룸 전용 필드 ===
    
    /**
     * 연결된 게임 ID (AI 게임룸용, 선택)
     */
    private String gameId;
    
    /**
     * 게임 설정 JSON (AI 게임룸용, 선택)
     */
    private String gameSettings;

    // === 플레이어 채팅룸 전용 필드 ===
    
    /**
     * 채팅 모드 (플레이어 채팅룸용, 선택)
     */
    private ChatMode chatMode;
    
    /**
     * 최대 용량 (플레이어 채팅룸용, 선택)
     */
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