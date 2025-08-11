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
    private String userId;
    private WorldType worldType;
    private Object data;
    private Instant timestamp;

    public enum MessageType {
        QUEUE_STATUS_UPDATE,     // 큐 상태 업데이트
        MATCHING_COMPLETE,       // 매칭 완료 알림
        MATCHING_CANCELLED,      // 매칭 취소 알림
        ERROR                    // 에러 발생
    }

    // 큐 상태 업데이트 메시지
    public static MatchingWebSocketMessage queueStatusUpdate(String userId, WorldType worldType, 
                                                           int currentPosition, int totalInQueue, 
                                                           long waitingTime) {
        QueueStatusData data = QueueStatusData.builder()
                .currentPosition(currentPosition)
                .totalInQueue(totalInQueue)
                .waitingTimeSeconds(waitingTime)
                .estimatedMessage(calculateEstimatedMessage(currentPosition))
                .build();

        return MatchingWebSocketMessage.builder()
                .type(MessageType.QUEUE_STATUS_UPDATE)
                .userId(userId)
                .worldType(worldType)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    // 매칭 완료 메시지
    public static MatchingWebSocketMessage matchingComplete(List<String> participants, WorldType worldType,
                                                          String gameSessionId, String aiGameRoomId, 
                                                          String chatRoomId) {
        MatchingCompleteData data = MatchingCompleteData.builder()
                .gameSessionId(gameSessionId)
                .aiGameRoomId(aiGameRoomId)
                .chatRoomId(chatRoomId)
                .participants(participants)
                .build();

        return MatchingWebSocketMessage.builder()
                .type(MessageType.MATCHING_COMPLETE)
                .worldType(worldType)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    // 매칭 취소 메시지
    public static MatchingWebSocketMessage matchingCancelled(String userId, WorldType worldType) {
        MatchingCancelData cancelData = MatchingCancelData.builder()
                .userId(userId)
                .worldType(worldType)
                .message("매칭이 취소되었습니다")
                .build();
                
        return MatchingWebSocketMessage.builder()
                .type(MessageType.MATCHING_CANCELLED)
                .userId(userId)
                .worldType(worldType)
                .data(cancelData)
                .timestamp(Instant.now())
                .build();
    }

    // 에러 메시지
    public static MatchingWebSocketMessage error(String userId, String errorMessage) {
        return MatchingWebSocketMessage.builder()
                .type(MessageType.ERROR)
                .userId(userId)
                .data(errorMessage)
                .timestamp(Instant.now())
                .build();
    }

    private static String calculateEstimatedMessage(int position) {
        if (position <= 1) {
            return "곧 매칭될 예정입니다";
        } else if (position == 2) {
            return "약 30초 후 매칭 예정";
        } else {
            int estimatedMinutes = (position / 3) + 1;
            return String.format("약 %d분 후 매칭 예정", estimatedMinutes);
        }
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
        private String userId;
        private WorldType worldType;
        private String message;
    }
}