package org.com.dungeontalk.domain.chat.dto;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.com.dungeontalk.domain.chat.entity.ChatRoom;

@Getter
@Setter
@Builder
public class ChatRoomDto {

    private String id;
    private String roomType;
    private String roomName;
    private String mode;
    private List<String> participants;
    private Instant createdAt;
    private Instant updatedAt;

    public static ChatRoomDto fromEntity(ChatRoom room) {
        return ChatRoomDto.builder()
            .id(room.getId())
            .roomType(room.getRoomType() != null ? room.getRoomType().name() : "UNKNOWN")
            .roomName(room.getRoomName() != null ? room.getRoomName() : "UNKNOWN")
            .mode(room.getMode().name())
            .createdAt(room.getCreatedAt())
            .updatedAt(room.getUpdatedAt())
            .build();
    }

}
