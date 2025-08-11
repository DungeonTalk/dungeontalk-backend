package org.com.dungeontalk.domain.chat.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.com.dungeontalk.domain.chat.common.MessageType;
import org.com.dungeontalk.domain.chat.common.Status;

@Getter
@Builder
public class PresenceMessageDto {

    private String roomId;
    private MessageType type;           // 항상 PRESENCE
    private long connectedCount;
    private List<MemberPresence> members;

    @Getter
    @Builder
    public static class MemberPresence {
        private String memberId;
        private String nickname;
        private Status status;          // ONLINE / OFFLINE
    }

    public static PresenceMessageDto of(String roomId, long count, List<MemberPresence> members) {
        return PresenceMessageDto.builder()
            .roomId(roomId)
            .type(MessageType.PRESENCE)
            .connectedCount(count)
            .members(members)
            .build();
    }

}
