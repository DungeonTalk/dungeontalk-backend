package org.com.dungeontalk.domain.auth.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthRedisManagerTest {

    @Mock
    @SuppressWarnings("unchecked")
    private RedisTemplate<String, String> sessionRedis;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Test
    @DisplayName("calculateRemainingExpiration: 토큰 만료까지 남은 ms를 계산한다")
    void calculateRemainingExpiration() {
        AuthRedisManager mgr = new AuthRedisManager(sessionRedis);

        String secret = "test-secret-key-0123456789-test-secret-key";
        ReflectionTestUtils.setField(mgr, "SECRET_KEY", secret);

        Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        long expMs = System.currentTimeMillis() + 60_000L;

        String token = Jwts.builder()
            .setSubject("m1")
            .setExpiration(new Date(expMs))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();

        Long remain = mgr.calculateRemainingExpiration(token);
        assertThat(remain).isBetween(1_000L, 60_000L);
    }

    @Test
    @DisplayName("블랙 리스트 키에 true 저장 + TTL 설정")
    void uploadAccessTokenToRedis() {
        when(sessionRedis.opsForValue()).thenReturn(valueOps);
        AuthRedisManager mgr = new AuthRedisManager(sessionRedis);

        mgr.uploadAccessTokenToRedis("blacklist:access:abc", 5000L);
        verify(valueOps).set("blacklist:access:abc", "true", 5000L, TimeUnit.MILLISECONDS);
    }

    @Test
    @DisplayName("refresh_token:{token} 키 삭제")
    void deleteRefreshToken() {
        AuthRedisManager mgr = new AuthRedisManager(sessionRedis);
        mgr.deleteRefreshToken("rt-123");
        verify(sessionRedis).delete("refresh_token:rt-123");
    }

}