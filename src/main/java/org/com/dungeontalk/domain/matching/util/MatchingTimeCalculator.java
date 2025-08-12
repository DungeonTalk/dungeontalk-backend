package org.com.dungeontalk.domain.matching.util;

import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.matching.common.MatchingConstants;
import org.com.dungeontalk.domain.matching.config.MatchingProperties;
import org.springframework.stereotype.Component;

/**
 * 매칭 대기 시간 계산 유틸리티
 */
@Component
@RequiredArgsConstructor
public class MatchingTimeCalculator {
    
    private final MatchingProperties matchingProperties;
    
    /**
     * 큐 내 위치를 기반으로 예상 대기 시간 메시지 계산
     * @param position 큐 내 위치 (1부터 시작)
     * @return 예상 대기 시간 메시지
     */
    public String calculateEstimatedWaitMessage(int position) {
        if (position <= 1) {
            return "곧 매칭될 예정입니다";
        } else if (position == 2) {
            int estimatedSeconds = matchingProperties.getTiming().getEstimatedSecondsPerMatch();
            return new StringBuilder()
                    .append("약 ")
                    .append(estimatedSeconds)
                    .append("초 후 매칭 예정")
                    .toString();
        } else {
            int estimatedMinutes = calculateEstimatedMinutes(position);
            return new StringBuilder()
                    .append("약 ")
                    .append(estimatedMinutes)
                    .append("분 후 매칭 예정")
                    .toString();
        }
    }
    
    /**
     * 현재 대기자 수를 기반으로 큐 상태 메시지 계산
     * @param currentWaiting 현재 대기자 수
     * @return 큐 상태 메시지
     */
    public String calculateQueueStatusMessage(int currentWaiting) {
        if (currentWaiting == 0) {
            return "대기자 없음";
        } else if (currentWaiting <= 2) {
            return "곧 매칭 가능";
        } else {
            int estimatedMinutes = calculateEstimatedMinutes(currentWaiting);
            return new StringBuilder()
                    .append("약 ")
                    .append(estimatedMinutes)
                    .append("분 대기")
                    .toString();
        }
    }
    
    /**
     * 위치/대기자 수를 기반으로 예상 대기 시간(분) 계산
     * @param count 위치 또는 대기자 수
     * @return 예상 대기 시간(분)
     */
    private int calculateEstimatedMinutes(int count) {
        // 3명씩 매칭되므로: (count / 3) + 1분
        // 예: 4명 대기 시 (4/3) + 1 = 2분
        return (count / MatchingConstants.REQUIRED_PARTICIPANTS) + 1;
    }
}