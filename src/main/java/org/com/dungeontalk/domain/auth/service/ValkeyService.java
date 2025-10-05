package org.com.dungeontalk.domain.auth.service;

import org.com.dungeontalk.domain.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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

    // 모든 키-값 조회
    public Map<String, Instant> getAllSessionKeyValues() {
        Set<String> keys = sessionRedis.keys("*");
        Map<String, Instant> result = new HashMap<>();

        if (keys != null && !keys.isEmpty()) {
            for (String key : keys) {
                String valueStr = sessionRedis.opsForValue().get(key);
                if (valueStr != null) {
                    try {
                        Instant value = Instant.parse(valueStr);
                        result.put(key, value);
                    } catch (DateTimeParseException e) {
                        // 파싱 불가능한 값은 무시하거나 로그 처리
                        System.out.println("Invalid Instant format for key: " + key + ", value: " + valueStr);
                    }
                }
            }
        }
        return result;
    }

    /* DEPRECATED CODE */
//    public Map<String, String> getAllSessionKeyValues() {
//        Set<String> keys = sessionRedis.keys("*");  // 모든 키 조회
//        Map<String, String> result = new HashMap<>();
//
//        if (keys != null && !keys.isEmpty()) {
//            for (String key : keys) {
//                String  valueStr  = sessionRedis.opsForValue().get(key);  // String 타입 값 조회
//                if ( valueStr  != null) {
//                    Instant value = Instant.parse( valueStr );  // String -> Instant 변환
//                    result.put(key, value);
//                }
//                // result.put(key, value);
//            }
//        }
//
//        return result;
//    }


    public Set<String> getAllSessionKeys() {
        // 모든 키 조회 (키만 반환)
        return sessionRedis.keys("*");
    }

    // AI 채팅 시스템용 메서드들 추가

    // 만료 시간과 함께 키-값 저장
    public void setWithExpiration(String key, String value, int timeoutSeconds) {
        sessionRedis.opsForValue().set(key, value, timeoutSeconds, TimeUnit.SECONDS);
    }

    // 키가 존재하지 않을 때만 설정 (락 구현용)
    public boolean setIfNotExists(String key, String value, int timeoutSeconds) {
        Boolean result = sessionRedis.opsForValue().setIfAbsent(key, value, timeoutSeconds, TimeUnit.SECONDS);
        return result != null && result;
    }

    // 키 삭제
    public void delete(String key) {
        sessionRedis.delete(key);
    }

    // 키 존재 여부 확인
    public boolean exists(String key) {
        Boolean result = sessionRedis.hasKey(key);
        return result != null && result;
    }

    // 키의 만료 시간 설정
    public void expire(String key, int timeoutSeconds) {
        sessionRedis.expire(key, timeoutSeconds, TimeUnit.SECONDS);
    }

    // 값 조회
    public String get(String key) {
        return sessionRedis.opsForValue().get(key);
    }

}
