package org.com.dungeontalk.domain.room.dto;

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

/**
 * 통합된 룸 응답 DTO
 * AI 게임룸과 플레이어 채팅룸 응답을 통합
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnifiedRoomResponse {

    // === 공통 필드 ===
    
    /**
     * 룸 ID
     */
    private String roomId;
    
    /**
     * 룸 타입
     */
    private RoomType roomType;
    
    /**
     * 룸 이름
     */
    private String roomName;
    
    /**
     * 룸 설명
     */
    private String description;
    
    /**
     * 룸 상태
     */
    private UnifiedRoomStatus status;
    
    /**
     * 현재 참여자 수
     */
    private int currentParticipants;
    
    /**
     * 최대 참여자 수
     */
    private int maxParticipants;
    
    /**
     * 참여자 ID 목록
     */
    private List<String> participantIds;
    
    /**
     * 생성 시간 (UTC)
     */
    private Instant createdAt;
    
    /**
     * 수정 시간 (UTC)
     */
    private Instant updatedAt;

    // === AI 게임룸 전용 필드 ===
    
    /**
     * 연결된 게임 ID
     */
    private String gameId;
    
    /**
     * 현재 턴 번호
     */
    private Integer currentTurn;
    
    /**
     * 게임 설정
     */
    private String gameSettings;
    
    /**
     * 마지막 활동 시간 (UTC)
     */
    private Instant lastActivity;

    // === 플레이어 채팅룸 전용 필드 ===
    
    /**
     * 최대 용량 (플레이어 채팅룸용)
     */
    private Long maxCapacity;
    
    /**
     * 현재 온라인 사용자 수
     */
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
                .description(aiResponse.getDescription())
                .status(mapAiGameStatus(aiResponse.getStatus()))
                .currentParticipants(aiResponse.getCurrentParticipantCount())
                .maxParticipants(aiResponse.getMaxParticipants())
                .participantIds(aiResponse.getParticipants())
                .createdAt(aiResponse.getCreatedAt() != null ? 
                          aiResponse.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toInstant() : null)
                // AI 게임룸 전용 필드
                .gameId(aiResponse.getGameId())
                .currentTurn(aiResponse.getCurrentTurn())
                .lastActivity(aiResponse.getLastActivity() != null ? 
                          aiResponse.getLastActivity().atZone(java.time.ZoneOffset.UTC).toInstant() : null)
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