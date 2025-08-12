package org.com.dungeontalk.domain.matching.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.com.dungeontalk.domain.matching.common.WorldType;

@Getter
@Builder
public class WorldQueueInfo {
    
    private WorldType worldType;
    private String displayName;
    private int currentWaiting;
    private int averageWaitTimeSeconds;
    private String estimatedWaitMessage;

    /**
     * WorldQueueInfo 생성을 위한 정적 팩토리 메서드
     */
    public static WorldQueueInfo of(WorldType worldType, int currentWaiting, 
                                  int averageWaitTime) {
        String waitMessage = calculateEstimatedWaitMessage(currentWaiting);

        return WorldQueueInfo.builder()
                .worldType(worldType)
                .displayName(worldType.getDisplayName())
                .currentWaiting(currentWaiting)
                .averageWaitTimeSeconds(averageWaitTime)
                .estimatedWaitMessage(waitMessage)
                .build();
    }

    /**
     * 현재 대기자 수를 기반으로 예상 대기 메시지 계산
     */
    private static String calculateEstimatedWaitMessage(int currentWaiting) {
        if (currentWaiting == 0) {
            return "대기자 없음";
        } else if (currentWaiting <= 2) {
            return "곧 매칭 가능";
        } else {
            int estimatedMinutes = (currentWaiting / 3) + 1;
            return String.format("약 %d분 대기", estimatedMinutes);
        }
    }
}