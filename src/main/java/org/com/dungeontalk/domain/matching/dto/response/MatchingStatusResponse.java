package org.com.dungeontalk.domain.matching.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.com.dungeontalk.domain.matching.common.MatchingStatus;
import org.com.dungeontalk.domain.matching.common.WorldType;
import org.com.dungeontalk.domain.matching.util.MatchingTimeCalculator;

import java.time.LocalDateTime;

@Getter
@Builder
public class MatchingStatusResponse {

    private String userId;
    private WorldType worldType;
    private MatchingStatus status;
    private int currentPosition;
    private int totalInQueue;
    private long waitingTimeSeconds;
    private String estimatedWaitTime;
    private LocalDateTime joinedAt;

    public static MatchingStatusResponse of(String userId, WorldType worldType, 
                                           MatchingStatus status, int queuePosition, 
                                           int totalInQueue, LocalDateTime joinedAt) {
        
        long waitingSeconds = java.time.Duration.between(joinedAt, LocalDateTime.now()).getSeconds();
        // MatchingTimeCalculator는 @Component이므로 static 메서드가 아님
        // 임시로 기존 로직 사용
        String estimatedTime = calculateEstimatedTime(queuePosition);
        
        return MatchingStatusResponse.builder()
                .userId(userId)
                .worldType(worldType)
                .status(status)
                .currentPosition(queuePosition)
                .totalInQueue(totalInQueue)
                .waitingTimeSeconds(waitingSeconds)
                .estimatedWaitTime(estimatedTime)
                .joinedAt(joinedAt)
                .build();
    }
    
    private static String calculateEstimatedTime(int position) {
        if (position <= 1) {
            return "곧 매칭될 예정입니다";
        } else if (position == 2) {
            return "약 30초 후 매칭 예정";
        } else {
            int estimatedMinutes = (position / 3) + 1;
            return "약 " + estimatedMinutes + "분 후 매칭 예정";
        }
    }
}