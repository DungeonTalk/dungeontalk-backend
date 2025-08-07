package org.com.dungeontalk.domain.chat.entity;

import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.com.dungeontalk.domain.chat.common.MessageType;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    private String messageId;    // UUID v7

    private String roomId;       // ★ 필수: 채팅방 ID (foreign key 역할)
    private String senderId;     // 보내는 사람
    private String receiverId;   // (선택) 받는 사람 (게임 채팅에서 사용)
    private String content;

    private MessageType type;     // JOIN, TALK, LEAVE

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
