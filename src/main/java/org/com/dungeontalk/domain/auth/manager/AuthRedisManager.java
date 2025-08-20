package org.com.dungeontalk.domain.auth.manager;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AuthRedisManager {

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    // 세션을 담당하는 Valkey 연결
    private final RedisTemplate<String, String> sessionRedis;
    public AuthRedisManager(
            @Qualifier("sessionRedisTemplate") RedisTemplate<String, String> sessionRedis) {
        this.sessionRedis = sessionRedis;
    }

    // 해당 엑세스 토큰의 남은 시간 계산
    public Long calculateRemainingExpiration(String accessToken) {
        Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(accessToken)
                .getBody();

        Date expiration = claims.getExpiration();
        long now = System.currentTimeMillis();
        long expirationTimeMs = expiration.getTime();

        return expirationTimeMs - now;
    }

    // 블랙리스트에 엑세스 토큰 업로드
    public void uploadAccessTokenToRedis(String key, long expiration){
        sessionRedis.opsForValue().set(key, "true", expiration, TimeUnit.MILLISECONDS);
    }

    // 이미 존재하는 리프레시 토큰 삭제
    public void deleteRefreshToken(String refreshToken){

        String refreshKey = "refresh_token:" + refreshToken;
        sessionRedis.delete(refreshKey);
    }

}
