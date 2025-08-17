package org.com.dungeontalk.domain.aichat.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주사위 굴림 요청 DTO
 */
@Getter
@NoArgsConstructor
public class DiceRollRequest {
    private String memberId;
    // MVP 이후 확장용 필드
    // private String diceType; // 예: "d20", "d6"
}
