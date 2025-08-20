package org.com.dungeontalk.domain.matching.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.com.dungeontalk.domain.worldtype.entity.WorldType;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class MatchingCompleteResponse {

    private String gameSessionId;
    private String aiGameRoomId;
    private String chatRoomId;
    private WorldType worldType;
    private List<String> participants;
    private Instant matchedAt;

    public static MatchingCompleteResponse of(String gameSessionId, String aiGameRoomId, 
                                            String chatRoomId, WorldType worldType, 
                                            List<String> participants) {
        return MatchingCompleteResponse.builder()
                .gameSessionId(gameSessionId)
                .aiGameRoomId(aiGameRoomId)
                .chatRoomId(chatRoomId)
                .worldType(worldType)
                .participants(participants)
                .matchedAt(Instant.now())
                .build();
    }
}