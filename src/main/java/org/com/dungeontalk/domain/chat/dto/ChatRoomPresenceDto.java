package org.com.dungeontalk.domain.chat.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatRoomPresenceDto {

    private String roomId;
    private int maxCapacity;                  // DB의 정원
    private long connectedCount;              // Redis 현재 접속자 수
    private List<MemberPresenceDto> members;  // 현재 접속 중인 멤버들
    private int participantCount;             // (선택) 누적 참여자 수 (Mongo 유지 시)
    private double occupancyRate;             // connectedCount / maxCapacity

    public static ChatRoomPresenceDto of(
        String roomId, int maxCapacity, long connectedCount,
        List<MemberPresenceDto> members, int participantCount
    ) {
        double rate = maxCapacity > 0 ? (double) connectedCount / (double) maxCapacity : 0.0;
        return ChatRoomPresenceDto.builder()
            .roomId(roomId)
            .maxCapacity(maxCapacity)
            .connectedCount(connectedCount)
            .members(members == null ? List.of() : members)
            .participantCount(participantCount)
            .occupancyRate(rate)
            .build();
    }

}
