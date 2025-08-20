package org.com.dungeontalk.domain.chat.entity;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.com.dungeontalk.domain.chat.common.ChatMode;
import org.com.dungeontalk.domain.chat.common.ChatRoomType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Builder(toBuilder = true)
public class ChatRoom {

    @Id
    private String id;

    @Indexed
    private String roomName;                 // ✅ 누락 보완

    private ChatRoomType roomType;           // PLAYER or GAME

    private ChatMode mode;                  // SINGLE or MULTI

    /** null 이면 미설정 → 서비스에서 defaultMaxCapacity 적용.
     *  0 또는 음수면 '무제한'으로 해석 (권장) */
    private Integer maxCapacity;

    /**
     * 도큐먼트 폭증/이중 저장 방지를 위해 영속화 대상에서 제외
     * (메시지는 별도 컬렉션 chat_messages 사용)
     * 필요 없다면 필드 자체를 제거하는 것을 권장
     */
    @Transient
    private List<ChatMessage> messages = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    /** 도메인 메서드(세터 대체) */
    public ChatRoom updateCapacity(Integer newCapacity, Instant now) {
        this.maxCapacity = newCapacity;
        this.updatedAt = now;
        return this;
    }

}
