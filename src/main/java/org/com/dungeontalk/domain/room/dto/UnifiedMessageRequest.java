package org.com.dungeontalk.domain.room.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.com.dungeontalk.domain.room.common.RoomType;
import org.com.dungeontalk.domain.room.common.UnifiedMessageType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 통합된 메시지 요청 DTO
 * AI 채팅과 플레이어 채팅의 메시지 요청을 통합
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnifiedMessageRequest {

    // === 공통 필드 ===
    
    /**
     * 룸 ID (필수)
     */
    @NotBlank(message = "룸 ID는 필수입니다")
    private String roomId;
    
    /**
     * 룸 타입 (필수)
     */
    @NotNull(message = "룸 타입은 필수입니다")
    private RoomType roomType;
    
    /**
     * 발신자 ID (필수)
     */
    @NotBlank(message = "발신자 ID는 필수입니다")
    private String senderId;
    
    /**
     * 메시지 타입 (필수)
     */
    @NotNull(message = "메시지 타입은 필수입니다")
    private UnifiedMessageType messageType;
    
    /**
     * 메시지 내용 (필수)
     */
    @NotBlank(message = "메시지 내용은 필수입니다")
    private String content;
    
    /**
     * 메시지 메타데이터 (선택)
     */
    private String metadata;

    // === AI 게임 전용 필드 ===
    
    /**
     * AI 게임룸 ID (AI 메시지용)
     */
    private String aiGameRoomId;
    
    /**
     * 게임 액션 타입 (AI 게임용)
     */
    private String gameActionType;
    
    /**
     * 턴 번호 (AI 게임용)
     */
    private Integer turnNumber;

    // === 플레이어 채팅 전용 필드 ===
    
    /**
     * 채팅룸 ID (플레이어 채팅용)
     */
    private String chatRoomId;
    
    /**
     * 발신자 닉네임 (플레이어 채팅용)
     */
    private String senderNickname;

    /**
     * AI 게임 메시지인지 확인
     */
    public boolean isAiGameMessage() {
        return roomType == RoomType.AI_GAME;
    }

    /**
     * 플레이어 채팅 메시지인지 확인
     */
    public boolean isPlayerChatMessage() {
        return roomType == RoomType.PLAYER_CHAT;
    }

    /**
     * 시스템 메시지인지 확인
     */
    public boolean isSystemMessage() {
        return messageType == UnifiedMessageType.SYSTEM;
    }

    /**
     * 사용자 메시지인지 확인
     */
    public boolean isUserMessage() {
        return messageType == UnifiedMessageType.USER;
    }

    /**
     * AI 게임용 필드 설정
     */
    public void setAiGameFields(String aiGameRoomId, String gameActionType, Integer turnNumber) {
        this.aiGameRoomId = aiGameRoomId;
        this.gameActionType = gameActionType;
        this.turnNumber = turnNumber;
    }

    /**
     * 플레이어 채팅용 필드 설정
     */
    public void setPlayerChatFields(String chatRoomId, String senderNickname) {
        this.chatRoomId = chatRoomId;
        this.senderNickname = senderNickname;
    }

    /**
     * 룸 타입에 따른 자동 필드 설정
     */
    public void autoSetFieldsByRoomType() {
        if (isAiGameMessage() && aiGameRoomId == null) {
            this.aiGameRoomId = this.roomId;
        } else if (isPlayerChatMessage() && chatRoomId == null) {
            this.chatRoomId = this.roomId;
        }
    }

    /**
     * 유효성 검증 - AI 게임 메시지
     */
    public void validateForAiGame() {
        if (!isAiGameMessage()) {
            return;
        }
        
        if (aiGameRoomId == null || aiGameRoomId.trim().isEmpty()) {
            this.aiGameRoomId = this.roomId;
        }
        
        // AI 게임 메시지 특별 검증 로직
        if (messageType.isPlayerChatType()) {
            throw new IllegalArgumentException("AI 게임에서는 플레이어 채팅 전용 메시지 타입을 사용할 수 없습니다: " + messageType);
        }
    }

    /**
     * 유효성 검증 - 플레이어 채팅 메시지
     */
    public void validateForPlayerChat() {
        if (!isPlayerChatMessage()) {
            return;
        }
        
        if (chatRoomId == null || chatRoomId.trim().isEmpty()) {
            this.chatRoomId = this.roomId;
        }
        
        // 플레이어 채팅 메시지 특별 검증 로직
        if (messageType.isAiGameType()) {
            throw new IllegalArgumentException("플레이어 채팅에서는 AI 게임 전용 메시지 타입을 사용할 수 없습니다: " + messageType);
        }
    }

    /**
     * 룸 타입에 따른 자동 검증
     */
    public void validateByRoomType() {
        autoSetFieldsByRoomType();
        
        if (isAiGameMessage()) {
            validateForAiGame();
        } else if (isPlayerChatMessage()) {
            validateForPlayerChat();
        }
    }
}