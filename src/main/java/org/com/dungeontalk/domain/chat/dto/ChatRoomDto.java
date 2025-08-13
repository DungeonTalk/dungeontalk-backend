package org.com.dungeontalk.domain.chat.dto;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.com.dungeontalk.domain.chat.common.ChatMode;
import org.com.dungeontalk.domain.chat.entity.ChatRoom;

@Getter
@Builder
public class ChatRoomDto {

    private String id;
    private String roomName;
    private ChatMode mode;
    private Instant createdAt;
    private Instant updatedAt;

    public static ChatRoomDto fromEntity(ChatRoom room) {
        return ChatRoomDto.builder()
            .id(room.getId())
            .roomName(room.getRoomName() != null ? room.getRoomName() : "UNKNOWN")
            .mode(room.getMode())
            .createdAt(room.getCreatedAt())
            .updatedAt(room.getUpdatedAt())
            .build();
    }

}
