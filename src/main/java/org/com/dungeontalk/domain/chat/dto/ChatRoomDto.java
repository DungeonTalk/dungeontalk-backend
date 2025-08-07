package org.com.dungeontalk.domain.chat.dto;

import java.time.LocalDateTime;
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

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ChatRoomDto fromEntity(ChatRoom room) {
        return ChatRoomDto.builder()
            .id(room.getId())
            .roomName(room.getRoomName())
            .roomType(room.getRoomType() != null ? room.getRoomType().name() : "UNKNOWN")
            .mode(room.getMode().name())
            .participants(room.getParticipants())
            .createdAt(room.getCreatedAt())
            .updatedAt(room.getUpdatedAt())
            .build();
    }

}
