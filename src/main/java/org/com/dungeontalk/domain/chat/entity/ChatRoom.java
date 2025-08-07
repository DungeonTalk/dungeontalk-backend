package org.com.dungeontalk.domain.chat.entity;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.com.dungeontalk.domain.chat.common.ChatMode;
import org.com.dungeontalk.domain.chat.common.ChatRoomType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
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

    private String roomName;

    @Enumerated(EnumType.STRING)
    private ChatRoomType roomType;          // PLAYER or GAME

    @Enumerated(EnumType.STRING)
    private ChatMode mode;                  // SINGLE or MULTI
    private List<String> participants;      // RDB 회원 ID

//    @Builder.Default
//    private List<ChatMessage> messages = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

}
