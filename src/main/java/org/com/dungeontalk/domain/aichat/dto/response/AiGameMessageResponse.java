package org.com.dungeontalk.domain.aichat.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.com.dungeontalk.domain.aichat.common.AiMessageType;
import org.com.dungeontalk.domain.aichat.entity.AiGameMessage;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AiGameMessageResponse {

    private String messageId;
    private String aiGameRoomId;
    private String senderId;
    private String senderNickname;
    private String content;
    private AiMessageType messageType;
    private int turnNumber;
    private int messageOrder;
    private Long aiResponseTime;
    private LocalDateTime createdAt;

    /**
     * AI 메시지 여부
     */
    public boolean isAiMessage() {
        return this.messageType == AiMessageType.AI;
    }

    /**
     * 시스템 메시지 여부
     */
    public boolean isSystemMessage() {
        return this.messageType == AiMessageType.SYSTEM ||
               this.messageType == AiMessageType.TURN_START ||
               this.messageType == AiMessageType.TURN_END;
    }

    /**
     * Entity에서 Response DTO로 변환
     */
    public static AiGameMessageResponse fromEntity(AiGameMessage message) {
        return AiGameMessageResponse.builder()
                .messageId(message.getId())
                .aiGameRoomId(message.getAiGameRoomId())
                .senderId(message.getSenderId())
                .senderNickname(message.getSenderNickname())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .turnNumber(message.getTurnNumber())
                .messageOrder(message.getMessageOrder())
                .aiResponseTime(message.getAiResponseTime())
                .createdAt(message.getCreatedAt())
                .build();
    }

    /**
     * DTO에서 Response DTO로 변환
     */
    public static AiGameMessageResponse fromDto(org.com.dungeontalk.domain.aichat.dto.AiGameMessageDto dto) {
        return AiGameMessageResponse.builder()
                .messageId(dto.getMessageId())
                .aiGameRoomId(dto.getAiGameRoomId())
                .senderId(dto.getSenderId())
                .senderNickname(dto.getSenderNickname())
                .content(dto.getContent())
                .messageType(dto.getMessageType())
                .turnNumber(dto.getTurnNumber())
                .messageOrder(dto.getMessageOrder())
                .aiResponseTime(dto.getAiResponseTime())
                .createdAt(dto.getCreatedAt())
                .build();
    }
}