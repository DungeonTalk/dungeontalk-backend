package org.com.dungeontalk.global.security;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JwtRedisService {

    private final RedisTemplate<String, String> sessionRedis;

    public JwtRedisService(@Qualifier("sessionRedisTemplate") RedisTemplate<String, String> sessionRedis) {
        this.sessionRedis = sessionRedis;
    }

        // 리프레쉬 토큰 세션 레디스에 저장 메서드
        public void saveRefreshTokenToSessionRedis (String memberId, String refreshToken){

            String key = "refresh_token:" + memberId;
            sessionRedis.opsForValue().set(key, refreshToken);
            //sessionRedis.opsForValue().set(key, refreshToken, REFRESH_TOKEN_EXPIRATION_TIME); -> 문제의 원인, REFRESH_TOKEN_EXPIRATION_TIME

        }

        //  블랙리스트에 있는지 확인
        public boolean isTokenBlacklisted (String token){
            String hashedKey = DigestUtils.sha256Hex(token);
            return sessionRedis.hasKey("blacklist:" + hashedKey);
        }

    }

