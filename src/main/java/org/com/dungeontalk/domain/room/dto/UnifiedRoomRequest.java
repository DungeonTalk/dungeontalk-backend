package org.com.dungeontalk.domain.room.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
@Setter
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
    private Long maxCapacity;

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
     * 유효성 검증 - AI 게임룸 필수 필드 체크
     */
    public void validateForAiGameRoom() {
        if (!isAiGameRoom()) {
            return;
        }
        
        // AI 게임룸 특별 검증 로직이 필요하면 여기에 추가
        if (maxParticipants == null) {
            maxParticipants = 3; // AI 게임룸 기본값
        }
    }

    /**
     * 유효성 검증 - 플레이어 채팅룸 필수 필드 체크
     */
    public void validateForPlayerChatRoom() {
        if (!isPlayerChatRoom()) {
            return;
        }
        
        // 플레이어 채팅룸 특별 검증 로직이 필요하면 여기에 추가
        if (chatMode == null) {
            chatMode = ChatMode.MULTI; // 기본값
        }
    }

    /**
     * 룸 타입에 따른 자동 검증
     */
    public void validateByRoomType() {
        if (isAiGameRoom()) {
            validateForAiGameRoom();
        } else if (isPlayerChatRoom()) {
            validateForPlayerChatRoom();
        }
    }
}