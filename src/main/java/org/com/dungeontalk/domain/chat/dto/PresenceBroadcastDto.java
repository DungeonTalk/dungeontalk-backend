package org.com.dungeontalk.domain.chat.dto;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PresenceBroadcastDto {
    private String roomId;
    private Integer maxCapacity;
    private long connectedCount;
    private List<PresenceMessageDto.MemberPresence> members; // 기존 타입 재활용
    private String eventType;           // JOIN, LEAVE, JOIN_IGNORED, LEAVE_IGNORED
    private Instant eventTime;

    public static PresenceBroadcastDto of(
        String roomId, Integer maxCapacity, long connectedCount,
        List<PresenceMessageDto.MemberPresence> members, String eventType
    ) {
        return PresenceBroadcastDto.builder()
            .roomId(roomId)
            .maxCapacity(maxCapacity)
            .connectedCount(connectedCount)
            .members(members)
            .eventType(eventType)
            .eventTime(Instant.now())
            .build();
    }
}

