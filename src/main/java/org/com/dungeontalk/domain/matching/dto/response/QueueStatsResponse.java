package org.com.dungeontalk.domain.matching.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.com.dungeontalk.domain.matching.common.WorldType;

import java.util.Map;

@Getter
@Builder
public class QueueStatsResponse {

    private Map<WorldType, WorldQueueInfo> queueInfo;
    private int totalWaiting;
    private String lastUpdated;

    @Getter
    @Builder
    public static class WorldQueueInfo {
        private WorldType worldType;
        private String displayName;
        private int currentWaiting;
        private int averageWaitTimeSeconds;
        private String estimatedWaitMessage;

        public static WorldQueueInfo of(WorldType worldType, int currentWaiting, 
                                      int averageWaitTime) {
            String waitMessage;
            if (currentWaiting == 0) {
                waitMessage = "대기자 없음";
            } else if (currentWaiting <= 2) {
                waitMessage = "곧 매칭 가능";
            } else {
                int estimatedMinutes = (currentWaiting / 3) + 1;
                waitMessage = String.format("약 %d분 대기", estimatedMinutes);
            }

            return WorldQueueInfo.builder()
                    .worldType(worldType)
                    .displayName(worldType.getDisplayName())
                    .currentWaiting(currentWaiting)
                    .averageWaitTimeSeconds(averageWaitTime)
                    .estimatedWaitMessage(waitMessage)
                    .build();
        }
    }
}