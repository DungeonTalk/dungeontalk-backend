package org.com.dungeontalk.domain.auth.manager;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class BruteForceManager {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${brute-force.enabled:true}")
    private boolean enabled;

    @Value("${brute-force.max-attempts:5}")
    private int maxAttempts;

    @Value("${brute-force.delay-enabled:true}")
    private boolean delayEnabled;

    @Value("${brute-force.cooldown-minutes:10}")
    private int cooldownMinutes;

    public BruteForceManager(@Qualifier("sessionRedisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 기본값 (properties에서 오버라이드 가능)
    private static final int MAX_IP_ATTEMPTS_PER_MINUTE = 20;

    // 로그인 시도 전 이상 행동 존재 유무 체크
    public void preCheck(String username, HttpServletRequest httpServletRequest) {

        // 로그인 디바이스 ip 추출
        String ip = httpServletRequest.getHeader("X-Forwarded-For");
        if (ip == null) ip = httpServletRequest.getRemoteAddr();

//        checkIpRateLimit(ip);
        checkAccountLock(username);
    }

    /**
     * 로그인 실패 시 방어 메서드
     *  - 로그인 실패 횟수 1~4 : Delay 방어
     *  - 로그인 실패 횟수 5 이상 : Cool Down 방어
     * @param username 유저의 아이디
     * @throws InterruptedException
     */
    public void loginFailed(String username) throws InterruptedException {
        if (!enabled) {
            log.debug("BruteForce protection disabled");
            return;
        }

        String failKey = "login_fail:" + username;
        String failCountStr = redisTemplate.opsForValue().get(failKey);
        int failCount = failCountStr != null ? Integer.parseInt(failCountStr) : 0;

        failCount++;
        redisTemplate.opsForValue().set(failKey, String.valueOf(failCount), Duration.ofMinutes(15));

        // 1~N회 실패 → Delay 방어 (설정 가능)
        if (failCount < maxAttempts) {
            if (delayEnabled) {
                long delayMillis = (long) Math.pow(2, failCount - 1) * 1000L;
                log.info("로그인 실패 ! {}유저는 {} ms동안 Delay가 됩니다.",username, delayMillis);
                Thread.sleep(delayMillis);
            }
        } else {
            // N회 이상 → Cool Down 방어
            String lockKey = "login_lock:" + username;
            Duration cooldown = Duration.ofMinutes(cooldownMinutes);
            redisTemplate.opsForValue().set(lockKey, "LOCKED", cooldown);
            log.warn("{} 유저는 {}회 이상 로그인을 실패하여 {}분간 계정이 정지됩니다.", username, maxAttempts, cooldownMinutes);
        }
    }

    // 로그인 성공 시 초기화
    public void loginSucceeded(String username) {
        redisTemplate.delete("login_fail:" + username);
        redisTemplate.delete("login_lock:" + username);
    }

    // 계정 잠금 상태 확인 메서드
    private void checkAccountLock(String username) {
        String lockKey = "login_lock:" + username;
        if (redisTemplate.hasKey(lockKey)) {
            throw new RuntimeException("계정이 잠금 상태입니다. 잠시 후 다시 시도하세요.");
        }
    }

    // Rate Limit 체크 메서드
    private void checkIpRateLimit(String ip) {
        String ipKey = "login_ip:" + ip;
        Long attempts = redisTemplate.opsForValue().increment(ipKey);
        if (attempts == 1) {
            redisTemplate.expire(ipKey, Duration.ofMinutes(10));
        }
        if (attempts > MAX_IP_ATTEMPTS_PER_MINUTE) {
            throw new RuntimeException(" 해당 IP에서 너무 많은 로그인 시도가 감지되었습니다.");
        }
    }
}