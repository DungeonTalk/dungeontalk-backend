package org.com.dungeontalk.domain.matching.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorldType {
    
    FANTASY("판타지", "중세 판타지 - 마법과 모험의 세계"),
    SF("SF", "미래 SF - 과학기술과 우주탐험"), 
    MODERN("현대", "현대 도시 - 일상과 미스터리");

    private final String displayName;
    private final String description;

    /**
     * 세계관별 AI 게임 설정 반환
     */
    public String getGameSettings() {
        return switch (this) {
            case FANTASY -> "중세 판타지 세계관에서 펼쳐지는 마법과 모험의 이야기";
            case SF -> "미래 우주 세계관에서 펼쳐지는 과학기술과 탐험의 이야기";
            case MODERN -> "현대 도시 세계관에서 펼쳐지는 일상과 미스터리의 이야기";
        };
    }

    /**
     * Redis 큐 키 생성
     */
    public String getQueueKey() {
        return "matching:queue:" + this.name();
    }

    /**
     * Redis 통계 키 생성
     */
    public String getStatsKey() {
        return "matching:stats:" + this.name();
    }
}