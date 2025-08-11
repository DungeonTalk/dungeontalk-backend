package org.com.dungeontalk.domain.chat.dto;

import lombok.Builder;
import lombok.Getter;
import org.com.dungeontalk.domain.chat.common.MessageType;

@Getter
@Builder
public class ConnectedCountMessageDto {
    private String roomId;
    private long connectedCount;
    private MessageType type;  // 항상 CONNECTED_COUNT

    public static ConnectedCountMessageDto of(String roomId, long count) {
        return ConnectedCountMessageDto.builder()
            .roomId(roomId)
            .connectedCount(count)
            .type(MessageType.CONNECTED_COUNT)
            .build();
    }
}
