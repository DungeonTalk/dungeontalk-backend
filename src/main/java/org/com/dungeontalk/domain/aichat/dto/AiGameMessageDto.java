package org.com.dungeontalk.domain.aichat.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.com.dungeontalk.domain.aichat.common.AiMessageType;
import org.com.dungeontalk.domain.aichat.entity.AiGameMessage;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AiGameMessageDto {

    private String messageId;
    private String aiGameRoomId;
    private String gameId;
    private String senderId;
    private String senderNickname;
    private String content;
    private AiMessageType messageType;
    private int turnNumber;
    private int messageOrder;
    private Long aiResponseTime;
    private String aiSources;
    private LocalDateTime createdAt;

    /**
     * Entity를 DTO로 변환
     */
    public static AiGameMessageDto fromEntity(AiGameMessage message) {
        return AiGameMessageDto.builder()
                .messageId(message.getId())
                .aiGameRoomId(message.getAiGameRoomId())
                .gameId(message.getGameId())
                .senderId(message.getSenderId())
                .senderNickname(message.getSenderNickname())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .turnNumber(message.getTurnNumber())
                .messageOrder(message.getMessageOrder())
                .aiResponseTime(message.getAiResponseTime())
                .aiSources(message.getAiSources())
                .createdAt(message.getCreatedAt())
                .build();
    }

    /**
     * AI 메시지 여부 확인
     */
    public boolean isAiMessage() {
        return this.messageType == AiMessageType.AI;
    }

    /**
     * 사용자 메시지 여부 확인
     */
    public boolean isUserMessage() {
        return this.messageType == AiMessageType.USER;
    }
}