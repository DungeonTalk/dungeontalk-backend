//package org.com.dungeontalk.domain.matching.service;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyMap;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.BDDMockito.then;
//
//import org.com.dungeontalk.domain.matching.common.MatchingConstants;
//import org.com.dungeontalk.domain.matching.common.WorldType;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.redis.core.HashOperations;
//import org.springframework.data.redis.core.ListOperations;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.data.redis.core.ValueOperations;
//
//import java.time.Duration;
//import java.util.HashMap;
//import java.util.Map;
//
//@ExtendWith(MockitoExtension.class)
//class MatchingQueueManagerTest {
//
//    @Mock
//    StringRedisTemplate redisTemplate;
//
//    @Mock
//    ListOperations<String, String> listOperations;
//
//    @Mock
//    HashOperations<String, Object, Object> hashOperations;
//
//    @Mock
//    ValueOperations<String, String> valueOperations;
//
//    @InjectMocks
//    MatchingQueueManager queueManager;
//
//    @Test
//    @DisplayName("사용자가 큐에 없는 경우 false 반환")
//    void isUserInQueue_false() {
//        // given
//        String memberId = "user-123";
//        String userKey = MatchingConstants.USER_KEY_PREFIX + memberId;
//
//        given(redisTemplate.hasKey(userKey)).willReturn(false);
//
//        // when
//        boolean result = queueManager.isUserInQueue(memberId);
//
//        // then
//        assertThat(result).isFalse();
//        then(redisTemplate).should().hasKey(userKey);
//    }
//
//    @Test
//    @DisplayName("사용자가 큐에 있는 경우 true 반환")
//    void isUserInQueue_true() {
//        // given
//        String memberId = "user-123";
//        String userKey = MatchingConstants.USER_KEY_PREFIX + memberId;
//
//        given(redisTemplate.hasKey(userKey)).willReturn(true);
//
//        // when
//        boolean result = queueManager.isUserInQueue(memberId);
//
//        // then
//        assertThat(result).isTrue();
//        then(redisTemplate).should().hasKey(userKey);
//    }
//
//    @Test
//    @DisplayName("큐에 사용자 추가 성공 테스트")
//    void addToQueue_success() {
//        // given
//        String memberId = "user-123";
//        WorldType worldType = WorldType.FANTASY;
//        String queueKey = worldType.getQueueKey();
//        String userKey = MatchingConstants.USER_KEY_PREFIX + memberId;
//
//        given(redisTemplate.opsForList()).willReturn(listOperations);
//        given(redisTemplate.opsForHash()).willReturn(hashOperations);
//        given(listOperations.size(queueKey)).willReturn(5L); // 현재 큐 크기
//
//        // when
//        queueManager.addToQueue(memberId, worldType);
//
//        // then
//        then(listOperations).should().leftPush(queueKey, memberId);
//        then(hashOperations).should().putAll(eq(userKey), anyMap());
//        then(redisTemplate).should().expire(eq(userKey), any(Duration.class));
//    }
//
//    @Test
//    @DisplayName("큐에서 사용자 제거 성공 테스트")
//    void removeFromQueue_success() {
//        // given
//        String memberId = "user-123";
//        WorldType worldType = WorldType.FANTASY;
//        String userKey = MatchingConstants.USER_KEY_PREFIX + memberId;
//        String queueKey = worldType.getQueueKey();
//
//        Map<Object, Object> userInfo = new HashMap<>();
//        userInfo.put("worldType", worldType.name());
//        userInfo.put("status", "WAITING");
//
//        given(redisTemplate.opsForHash()).willReturn(hashOperations);
//        given(redisTemplate.opsForList()).willReturn(listOperations);
//        given(hashOperations.entries(userKey)).willReturn(userInfo);
//        given(listOperations.remove(queueKey, 0, memberId)).willReturn(1L);
//
//        // when
//        boolean result = queueManager.removeFromQueue(memberId);
//
//        // then
//        assertThat(result).isTrue();
//        then(listOperations).should().remove(queueKey, 0, memberId);
//        then(redisTemplate).should().delete(userKey);
//    }
//
//    @Test
//    @DisplayName("큐에서 사용자 제거 실패 테스트 - 사용자 정보 없음")
//    void removeFromQueue_failure_noUserInfo() {
//        // given
//        String memberId = "user-123";
//        String userKey = MatchingConstants.USER_KEY_PREFIX + memberId;
//
//        given(redisTemplate.opsForHash()).willReturn(hashOperations);
//        given(hashOperations.entries(userKey)).willReturn(new HashMap<>());
//
//        // when
//        boolean result = queueManager.removeFromQueue(memberId);
//
//        // then
//        assertThat(result).isFalse();
//        then(redisTemplate).shouldHaveNoMoreInteractions();
//    }
//
//    @Test
//    @DisplayName("큐 크기 조회 테스트")
//    void getQueueSize_test() {
//        // given
//        WorldType worldType = WorldType.FANTASY;
//        String queueKey = worldType.getQueueKey();
//        Long expectedSize = 10L;
//
//        given(redisTemplate.opsForList()).willReturn(listOperations);
//        given(listOperations.size(queueKey)).willReturn(expectedSize);
//
//        // when
//        int result = queueManager.getQueueSize(worldType);
//
//        // then
//        assertThat(result).isEqualTo(10);
//        then(listOperations).should().size(queueKey);
//    }
//
//    @Test
//    @DisplayName("큐 크기 조회 테스트 - null 반환시 0")
//    void getQueueSize_null_returns_zero() {
//        // given
//        WorldType worldType = WorldType.FANTASY;
//        String queueKey = worldType.getQueueKey();
//
//        given(redisTemplate.opsForList()).willReturn(listOperations);
//        given(listOperations.size(queueKey)).willReturn(null);
//
//        // when
//        int result = queueManager.getQueueSize(worldType);
//
//        // then
//        assertThat(result).isEqualTo(0);
//        then(listOperations).should().size(queueKey);
//    }
//
//    @Test
//    @DisplayName("매칭 가능 여부 확인 테스트 - 3명 이상인 경우")
//    void canProcessMatching_true() {
//        // given
//        WorldType worldType = WorldType.FANTASY;
//        String queueKey = worldType.getQueueKey();
//
//        given(redisTemplate.opsForList()).willReturn(listOperations);
//        given(listOperations.size(queueKey)).willReturn(5L);
//
//        // when
//        boolean result = queueManager.canProcessMatching(worldType);
//
//        // then
//        assertThat(result).isTrue();
//        then(listOperations).should().size(queueKey);
//    }
//
//    @Test
//    @DisplayName("매칭 가능 여부 확인 테스트 - 3명 미만인 경우")
//    void canProcessMatching_false() {
//        // given
//        WorldType worldType = WorldType.FANTASY;
//        String queueKey = worldType.getQueueKey();
//
//        given(redisTemplate.opsForList()).willReturn(listOperations);
//        given(listOperations.size(queueKey)).willReturn(2L);
//
//        // when
//        boolean result = queueManager.canProcessMatching(worldType);
//
//        // then
//        assertThat(result).isFalse();
//        then(listOperations).should().size(queueKey);
//    }
//}