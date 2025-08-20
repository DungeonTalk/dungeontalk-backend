package org.com.dungeontalk.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.com.dungeontalk.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class ValkeyServiceTest {

    @Test
    @DisplayName("세션 Redis에 key, value 저장")
    void saveSessionData() {
        RedisTemplate<String, String> cache = mock(RedisTemplate.class);
        RedisTemplate<String, String> session = mock(RedisTemplate.class);
        MemberRepository memberRepo = mock(MemberRepository.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);

        when(session.opsForValue()).thenReturn(ops);

        ValkeyService valkeyService = new ValkeyService(cache, session, memberRepo);
        valkeyService.saveSessionData("key","value");

        verify(ops).set("key","value");
    }

    @Test
    @DisplayName("키 전체 조회 + Instant 파싱 (잘못된 값은 무시함)")
    void getAllSessionKeyValues() {
        RedisTemplate<String, String> cache = mock(RedisTemplate.class);
        RedisTemplate<String, String> session = mock(RedisTemplate.class);
        MemberRepository memberRepo = mock(MemberRepository.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);

        when(session.keys("*")).thenReturn(Set.of("k1","k2","bad"));
        when(session.opsForValue()).thenReturn(ops);
        when(ops.get("k1")).thenReturn(Instant.now().toString());
        when(ops.get("k2")).thenReturn(Instant.now().plusSeconds(10).toString());
        when(ops.get("bad")).thenReturn("not-an-instant");

        ValkeyService valkeyService = new ValkeyService(cache, session, memberRepo);
        Map<String, Instant> out = valkeyService.getAllSessionKeyValues();

        assertThat(out).containsKeys("k1","k2");
        assertThat(out).doesNotContainKey("bad");
    }

    @Test
    @DisplayName("exists/expire/delete/get/setIfNotExists/setWithExpiration 호출 검증하기")
    void basicOps() {
        RedisTemplate<String,String> cache = mock(RedisTemplate.class);
        RedisTemplate<String,String> session = mock(RedisTemplate.class);
        MemberRepository memberRepo = mock(MemberRepository.class);
        ValueOperations<String,String> ops = mock(ValueOperations.class);

        when(session.opsForValue()).thenReturn(ops);
        when(session.hasKey("key")).thenReturn(true);
        when(ops.setIfAbsent(eq("lock"), eq("1"),
            eq(5L), eq(java.util.concurrent.TimeUnit.SECONDS))).thenReturn(Boolean.TRUE);
        when(ops.get("key")).thenReturn("value");

        ValkeyService svc = new ValkeyService(cache, session, memberRepo);

        svc.setWithExpiration("a","b",3);
        verify(ops).set("a","b",3, java.util.concurrent.TimeUnit.SECONDS);

        boolean locked = svc.setIfNotExists("lock","1",5);
        assertThat(locked).isTrue();

        boolean exists = svc.exists("key");
        assertThat(exists).isTrue();

        svc.expire("key",7);
        verify(session).expire("key",7L, java.util.concurrent.TimeUnit.SECONDS);

        String value = svc.get("key");
        assertThat(value).isEqualTo("value");

        svc.delete("key");
        verify(session).delete("key");
    }

}