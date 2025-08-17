package org.com.dungeontalk.domain.aichat.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 주사위 굴림 로직을 담당하는 서비스
 * 게임의 핵심 규칙을 담당하며, 다른 서비스에서 호출하여 사용합니다.
 */
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
}
