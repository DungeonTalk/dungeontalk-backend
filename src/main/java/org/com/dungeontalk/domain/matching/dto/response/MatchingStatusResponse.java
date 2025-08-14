package org.com.dungeontalk.domain.matching.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.com.dungeontalk.domain.matching.common.MatchingStatus;
import org.com.dungeontalk.domain.matching.common.WorldType;
import org.com.dungeontalk.domain.matching.util.MatchingTimeCalculator;

import java.time.Instant;

@Schema(description = "매칭 상태 응답")
@Getter
@Builder
public class MatchingStatusResponse {

    @Schema(description = "회원 ID", example = "user-12345")
    private String memberId;
    
    @Schema(description = "세계관 타입", example = "FANTASY")
    private WorldType worldType;
    
    @Schema(description = "매칭 상태", example = "WAITING")
    private MatchingStatus status;
    
    @Schema(description = "현재 대기 순서", example = "3")
    private int currentPosition;
    
    @Schema(description = "전체 대기자 수", example = "8")
    private int totalInQueue;
    
    @Schema(description = "대기 시간 (초)", example = "45")
    private long waitingTimeSeconds;
    
    @Schema(description = "예상 대기 시간", example = "약 1분 후 매칭 예정")
    private String estimatedWaitTime;
    
    @Schema(description = "매칭 참가 시간")
    private Instant joinedAt;

    public static MatchingStatusResponse of(String memberId, WorldType worldType, 
                                           MatchingStatus status, int queuePosition, 
                                           int totalInQueue, Instant joinedAt) {
        
        long waitingSeconds = java.time.Duration.between(joinedAt, Instant.now()).getSeconds();
        // MatchingTimeCalculator는 @Component이므로 static 메서드가 아님
        // 임시로 기존 로직 사용
        String estimatedTime = calculateEstimatedTime(queuePosition);
        
        return MatchingStatusResponse.builder()
                .memberId(memberId)
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