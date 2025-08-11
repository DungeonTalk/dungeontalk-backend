package org.com.dungeontalk.domain.chat.entity;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.com.dungeontalk.domain.chat.common.Status;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "chat_room_members")
@CompoundIndex(name = "uniq_room_member", def = "{'roomId': 1, 'memberId': 1}", unique = true) // ✅ 중복 방지
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomMember {
    @Id
    private String id;          // MongoDB ObjectId

    @Indexed
    private String roomId;      // ChatRoom의 id

    @Indexed
    private String memberId;    // PostgreSQL Member의 id (연관)

    @Enumerated(EnumType.STRING)
    private Status status;      // ONLINE, OFFLINE

    @CreatedDate
    private Instant joinedAt;

    @LastModifiedDate
    private Instant updatedAt;

    private Instant leftAt;
}
