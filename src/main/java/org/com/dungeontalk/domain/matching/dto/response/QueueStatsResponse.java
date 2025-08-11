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
}