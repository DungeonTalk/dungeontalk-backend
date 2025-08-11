package org.com.dungeontalk.domain.chat.entity;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.com.dungeontalk.domain.chat.common.ChatMode;
import org.com.dungeontalk.domain.chat.common.ChatRoomType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "chat_rooms")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Builder
public class ChatRoom {

    @Id
    private String id;

    @Indexed
    private String roomName;                 // ✅ 누락 보완

    @Enumerated(EnumType.STRING)
    private ChatRoomType roomType;           // PLAYER or GAME

    @Enumerated(EnumType.STRING)
    private ChatMode mode;                  // SINGLE or MULTI

//    private List<String> participants;      // RDB 회원 ID

    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

}
