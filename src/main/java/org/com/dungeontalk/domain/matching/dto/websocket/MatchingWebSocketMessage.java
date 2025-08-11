package org.com.dungeontalk.domain.matching.dto.websocket;

import lombok.Builder;
import lombok.Getter;
import org.com.dungeontalk.domain.matching.common.WorldType;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class MatchingWebSocketMessage {

    private MessageType type;
    private String memberId;
    private WorldType worldType;
    private Object data;
    private Instant timestamp;

    public enum MessageType {
        QUEUE_STATUS_UPDATE,     // 큐 상태 업데이트
        MATCHING_COMPLETE,       // 매칭 완료 알림
        MATCHING_CANCELLED,      // 매칭 취소 알림
        ERROR                    // 에러 발생
    }


    @Getter
    @Builder
    public static class QueueStatusData {
        private int currentPosition;
        private int totalInQueue;
        private long waitingTimeSeconds;
        private String estimatedMessage;
    }

    @Getter
    @Builder
    public static class MatchingCompleteData {
        private String gameSessionId;
        private String aiGameRoomId;
        private String chatRoomId;
        private List<String> participants;
    }

    @Getter
    @Builder
    public static class MatchingCancelData {
        private String memberId;
        private WorldType worldType;
        private String message;
    }
}