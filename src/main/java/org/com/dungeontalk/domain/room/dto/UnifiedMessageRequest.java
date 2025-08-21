package org.com.dungeontalk.domain.room.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.com.dungeontalk.domain.room.common.RoomType;
import org.com.dungeontalk.domain.room.common.UnifiedMessageType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 통합된 메시지 요청 DTO
 * AI 채팅과 플레이어 채팅의 메시지 요청을 통합
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
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
    
    /**
     * 캐릭터 스탯 정보 (AI 게임용)
     */
    private Object characterStats;

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
     * AI 게임 메시지 생성을 위한 빌더
     */
    public static UnifiedMessageRequestBuilder aiGameMessage() {
        return UnifiedMessageRequest.builder()
                .roomType(RoomType.AI_GAME)
                .messageType(UnifiedMessageType.USER);
    }

    /**
     * 플레이어 채팅 메시지 생성을 위한 빌더
     */
    public static UnifiedMessageRequestBuilder playerChatMessage() {
        return UnifiedMessageRequest.builder()
                .roomType(RoomType.PLAYER_CHAT)
                .messageType(UnifiedMessageType.USER);
    }

    /**
     * 시스템 메시지 생성을 위한 빌더
     */
    public static UnifiedMessageRequestBuilder systemMessage() {
        return UnifiedMessageRequest.builder()
                .messageType(UnifiedMessageType.SYSTEM);
    }

    /**
     * 자동으로 roomId에 맞는 전용 필드 설정
     */
    public UnifiedMessageRequest withAutoFields() {
        return this.toBuilder()
                .aiGameRoomId(isAiGameMessage() ? roomId : aiGameRoomId)
                .chatRoomId(isPlayerChatMessage() ? roomId : chatRoomId)
                .build();
    }

    /**
     * 룸 타입에 따른 유효성 검증
     */
    public void validateByRoomType() {
        if (roomType == null) {
            throw new IllegalArgumentException("룸 타입은 필수입니다");
        }
        
        if (messageType == null) {
            throw new IllegalArgumentException("메시지 타입은 필수입니다");
        }
        
        if (roomId == null || roomId.trim().isEmpty()) {
            throw new IllegalArgumentException("룸 ID는 필수입니다");
        }
        
        if (senderId == null || senderId.trim().isEmpty()) {
            throw new IllegalArgumentException("발신자 ID는 필수입니다");
        }
        
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("메시지 내용은 필수입니다");
        }
    }
}