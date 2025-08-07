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


    /**
     * Redis에 저장된 모든 키와 값을 Map 형태로 반환
     */
    public Map<String, String> getAllSessionRedisData() {
        Map<String, String> data = new HashMap<>();

        // 모든 키 조회 (주의: 대규모 데이터일 경우 성능 문제 발생 가능)
        Set<String> keys = sessionRedis.keys("*");

        if (keys != null && !keys.isEmpty()) {
            for (String key : keys) {
                String value = sessionRedis.opsForValue().get(key);
                data.put(key, value);
            }
        }

        return data;
    }
}
