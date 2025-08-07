package org.com.dungeontalk.domain.chat.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.com.dungeontalk.domain.chat.common.MessageType;
import org.com.dungeontalk.domain.chat.entity.ChatMessage;

@Getter
@Setter
@Builder
public class ChatMessageDto {

    private String messageId;
    private String roomId;
    private String senderId;
    private String receiverId;
    private String senderNickName;
    private String content;
    private MessageType type;
    private LocalDateTime createdAt;

    // 닉네임 포함 변환
    public static ChatMessageDto fromEntity(ChatMessage msg, String senderNickname) {
        return ChatMessageDto.builder()
            .messageId(msg.getMessageId())
            .roomId(msg.getRoomId())
            .senderId(msg.getSenderId())
            .receiverId(msg.getReceiverId())
            .senderNickName(senderNickname)
            .content(msg.getContent())
            .type(msg.getType())
            .createdAt(msg.getCreatedAt())
            .build();
    }
}
