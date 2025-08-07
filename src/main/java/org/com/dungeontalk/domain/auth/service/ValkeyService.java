package org.com.dungeontalk.domain.auth.service;

import org.com.dungeontalk.domain.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class ValkeyService {

    private final RedisTemplate<String, String> cacheRedis;
    private final RedisTemplate<String, String> sessionRedis;
    private final MemberRepository memberRepository;

    public ValkeyService(
            @Qualifier("cacheRedisTemplate") RedisTemplate<String, String> cacheRedis,
            @Qualifier("sessionRedisTemplate") RedisTemplate<String, String> sessionRedis,
            MemberRepository memberRepository) {
        this.cacheRedis = cacheRedis;
        this.sessionRedis = sessionRedis;
        this.memberRepository = memberRepository;
    }

    // Redis 세션에 단일 키-값 저장
    public void saveSessionData(String key, String value) {
        sessionRedis.opsForValue().set(key, value);
    }

    // Redis 세션에 저장된 test키 모두 조회
    public Map<String, String> getAllTestKeySessionData() {
        String pattern = "test-key*";  // test-key로 시작하는 모든 키 조회
        Set<String> keys = sessionRedis.keys(pattern);
        Map<String, String> result = new HashMap<>();

        if (keys != null) {
            for (String key : keys) {
                String value = sessionRedis.opsForValue().get(key);  // String 타입 값 조회
                result.put(key, value);
            }
        }

        return result;
    }


    public Set<String> getAllSessionKeys() {
        // 모든 키 조회 (키만 반환)
        return sessionRedis.keys("*");
    }


}
