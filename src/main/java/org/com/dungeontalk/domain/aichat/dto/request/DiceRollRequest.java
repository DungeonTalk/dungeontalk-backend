package org.com.dungeontalk.domain.aichat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

/**
 * 주사위 굴림 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Jacksonized
public class DiceRollRequest {
    private String memberId;
    
    /**
     * 사용할 스탯 타입
     * 가능한 값: "str", "dex", "int", "wis", "wil", "luk"
     */
    private String statType;
    
    /**
     * 어떤 행동인지 설명 (선택사항)
     * 예: "자물쇠 따기", "근력 판정", "지식 체크"
     */
    private String action;
    
    // MVP 이후 확장용 필드
    // private String diceType; // 예: "d20", "d6" (현재는 d20 고정)
    // private Integer dc;      // 난이도 (Difficulty Class)
}
