package org.com.dungeontalk.domain.aichat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 주사위 굴림 로직을 담당하는 서비스
 * 게임의 핵심 규칙을 담당하며, 다른 서비스에서 호출하여 사용합니다.
 */
@Slf4j
@Service
public class DiceService {

    /**
     * 주어진 면 수의 주사위를 굴립니다. (예: d20 -> sides=20)
     * @param sides 주사위 면 수
     * @return 1부터 sides까지의 랜덤 정수
     */
    public int roll(int sides) {
        if (sides <= 0) {
            // 잘못된 입력에 대한 방어 코드
            return 1;
        }
        // ThreadLocalRandom은 동시성 환경에서 더 나은 성능을 보입니다.
        return ThreadLocalRandom.current().nextInt(1, sides + 1);
    }

    /**
     * 스탯값으로부터 보정치를 계산합니다.
     * D&D 5e 표준 공식: (스탯 - 10) / 2
     * @param statValue 스탯값 (보통 1~20 범위)
     * @return 보정치 (-5 ~ +5 범위)
     */
    public int calculateModifier(int statValue) {
        return (statValue - 10) / 2;
    }

    /**
     * 스탯 보정치가 적용된 주사위를 굴립니다.
     * @param sides 주사위 면 수
     * @param statValue 적용할 스탯값
     * @return 주사위 결과 + 보정치
     */
    public DiceResult rollWithModifier(int sides, int statValue) {
        int baseRoll = roll(sides);
        int modifier = calculateModifier(statValue);
        int finalResult = baseRoll + modifier;
        
        log.debug("주사위 굴림: d{} = {}, 스탯 {} (보정: {:+d}) -> 최종 {}", 
                 sides, baseRoll, statValue, modifier, finalResult);
        
        return new DiceResult(baseRoll, modifier, finalResult);
    }

    /**
     * 주사위 굴림 결과를 담는 클래스
     */
    public static class DiceResult {
        private final int baseRoll;     // 기본 주사위 결과
        private final int modifier;    // 스탯 보정치
        private final int finalResult; // 최종 결과

        public DiceResult(int baseRoll, int modifier, int finalResult) {
            this.baseRoll = baseRoll;
            this.modifier = modifier;
            this.finalResult = finalResult;
        }

        public int getBaseRoll() { return baseRoll; }
        public int getModifier() { return modifier; }
        public int getFinalResult() { return finalResult; }
        
        public String getModifierString() {
            return modifier >= 0 ? "+" + modifier : String.valueOf(modifier);
        }
    }
}
