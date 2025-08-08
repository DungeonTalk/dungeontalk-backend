package org.com.dungeontalk.domain.aichat.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.com.dungeontalk.domain.aichat.common.AiMessageType;

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
}