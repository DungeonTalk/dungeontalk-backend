package org.com.dungeontalk.domain.auth.manager;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class BruteForceManagerTest {

    @Mock
    @SuppressWarnings("unchecked")
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private HttpServletRequest request;

    @Test
    @DisplayName("IP rate limit 초과 시 RuntimeException 발생")
    void preCheck_rateLimitExceeded() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4");
        when(valueOps.increment("login_ip:1.2.3.4")).thenReturn(21L); // 임계 초과

        BruteForceManager mgr = new BruteForceManager(redisTemplate);

        assertThatThrownBy(() -> mgr.preCheck("alice", request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("너무 많은 로그인 시도");
    }

    @Test
    @DisplayName("계정 잠금 상태이면 RuntimeException 발생")
    void preCheck_accountLocked() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(redisTemplate.hasKey("login_lock:alice")).thenReturn(true);

        BruteForceManager mgr = new BruteForceManager(redisTemplate);

        assertThatThrownBy(() -> mgr.preCheck("alice", request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("계정이 잠금 상태");
    }

    @Test
    @DisplayName("loginFailed: 기존 실패횟수 4에서 5로 증가하고 계정 잠금(쿨다운) 설정")
    void loginFailed_lockdownBranch_noSleep() throws InterruptedException {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("login_fail:alice")).thenReturn("4"); // 4에서 시작 → 5로 증가

        BruteForceManager mgr = new BruteForceManager(redisTemplate);

        mgr.loginFailed("alice");

        // 실패횟수 증가 + TTL(15분)
        verify(valueOps).set(eq("login_fail:alice"), eq("5"), eq(Duration.ofMinutes(15)));

        // 잠금 키 설정
        verify(valueOps).set(eq("login_lock:alice"), eq("LOCKED"), any(Duration.class));
    }

    @Test
    @DisplayName("loginSucceeded: 실패/잠금 키 삭제")
    void loginSucceeded() {
        BruteForceManager mgr = new BruteForceManager(redisTemplate);
        mgr.loginSucceeded("alice");
        verify(redisTemplate).delete("login_fail:alice");
        verify(redisTemplate).delete("login_lock:alice");
    }

}