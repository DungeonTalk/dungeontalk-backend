package org.com.dungeontalk.domain.matching.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.com.dungeontalk.domain.matching.common.WorldType;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class MatchingCompleteResponse {

    private String gameSessionId;
    private String aiGameRoomId;
    private String chatRoomId;
    private WorldType worldType;
    private List<String> participants;
    private LocalDateTime matchedAt;

    public static MatchingCompleteResponse of(String gameSessionId, String aiGameRoomId, 
                                            String chatRoomId, WorldType worldType, 
                                            List<String> participants) {
        return MatchingCompleteResponse.builder()
                .gameSessionId(gameSessionId)
                .aiGameRoomId(aiGameRoomId)
                .chatRoomId(chatRoomId)
                .worldType(worldType)
                .participants(participants)
                .matchedAt(LocalDateTime.now())
                .build();
    }
}