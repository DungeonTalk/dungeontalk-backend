package org.com.dungeontalk.domain.auth.manager;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class LoginAttemptManager {

    private final RedisTemplate<String, String> redisTemplate;

    public LoginAttemptManager(@Qualifier("sessionRedisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 설정
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration COOLDOWN_DURATION = Duration.ofMinutes(10);

    // 로그인 시도 전 체크
    public void preCheck(String username, String ip) {
        checkIpRateLimit(ip);
        checkAccountLock(username);
    }

    // 실패 기록 + 딜레이/잠금 적용
    public void loginFailed(String username) throws InterruptedException {
        String failKey = "login_fail:" + username;
        String failCountStr = redisTemplate.opsForValue().get(failKey);
        int failCount = failCountStr != null ? Integer.parseInt(failCountStr) : 0;

        failCount++;
        redisTemplate.opsForValue().set(failKey, String.valueOf(failCount), Duration.ofMinutes(15));

        // 1~4회 실패 → 딜레이
        if (failCount < MAX_ATTEMPTS) {
            long delayMillis = (long) Math.pow(2, failCount - 1) * 1000L; // 1,2,4,8초
            log.info("Login fail delay {} ms for user {}", delayMillis, username);
            Thread.sleep(delayMillis);
        } else {
            // 5회 이상 → 잠금
            String lockKey = "login_lock:" + username;
            redisTemplate.opsForValue().set(lockKey, "LOCKED", COOLDOWN_DURATION);
            log.warn("User {} is locked for {} minutes", username, COOLDOWN_DURATION.toMinutes());
        }
    }

    // 로그인 성공 시 초기화
    public void loginSucceeded(String username) {
        redisTemplate.delete("login_fail:" + username);
        redisTemplate.delete("login_lock:" + username);
    }

    private void checkAccountLock(String username) {
        String lockKey = "login_lock:" + username;
        if (redisTemplate.hasKey(lockKey)) {
            throw new RuntimeException("계정이 잠금 상태입니다. 잠시 후 다시 시도하세요.");
        }
    }

    // IP Rate Limit 예시
    private static final int MAX_IP_ATTEMPTS_PER_MINUTE = 20;

    private void checkIpRateLimit(String ip) {
        String ipKey = "login_ip:" + ip;
        Long attempts = redisTemplate.opsForValue().increment(ipKey);
        if (attempts == 1) {
            redisTemplate.expire(ipKey, Duration.ofMinutes(10));
        }
        if (attempts > MAX_IP_ATTEMPTS_PER_MINUTE) {
            throw new RuntimeException("IP에서 너무 많은 로그인 시도가 감지되었습니다.");
        }
    }
}