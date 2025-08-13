package org.com.dungeontalk.domain.chat.entity;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.com.dungeontalk.domain.chat.common.MessageType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "chat_messages")
@CompoundIndex(name = "room_created_idx", def = "{'roomId': 1, 'createdAt': 1}")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
public class ChatMessage {

    @Id
    private String messageId;    // UUID v7

    @Indexed
    private String roomId;       // ★ 필수: 채팅방 ID (foreign key 역할)
    private String senderId;     // 보내는 사람
    private String receiverId;   // (선택) 받는 사람 (게임 채팅에서 사용)
    private String content;

    private MessageType type;     // JOIN, TALK, LEAVE

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

}
