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
}
