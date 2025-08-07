package org.com.dungeontalk.domain.chat.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.com.dungeontalk.domain.chat.common.MessageType;

@Getter
@Setter
public class ChatMessageSendRequestDto {
    private String messageId;
    private String roomId;              // 클라이언트에서 보내야 함
    private String senderId;
    private String senderNickname;
    private String receiverId;
    private String content;
    private MessageType type;

}
