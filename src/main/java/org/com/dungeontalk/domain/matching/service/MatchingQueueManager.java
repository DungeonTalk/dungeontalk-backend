package org.com.dungeontalk.domain.matching.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.matching.common.MatchingConstants;
import org.com.dungeontalk.domain.matching.common.MatchingStatus;
import org.com.dungeontalk.domain.matching.common.WorldType;
import org.com.dungeontalk.domain.matching.exception.MatchingException;
import org.com.dungeontalk.global.exception.ErrorCode;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingQueueManager {

    private final StringRedisTemplate redisTemplate;

    // Lua 스크립트: 3명 추출 (원자성 보장)
    private static final String EXTRACT_USERS_SCRIPT = """
        local queueKey = KEYS[1]
        local queueSize = redis.call('LLEN', queueKey)
        
        if queueSize < 3 then
            return nil
        end
        
        local users = {}
        for i = 1, 3 do
            local user = redis.call('RPOP', queueKey)
            if user then
                table.insert(users, user)
            end
        end
        
        return users
        """;

    /**
     * 사용자가 이미 큐에 있는지 확인
     */
    public boolean isUserInQueue(String memberId) {
        String userKey = MatchingConstants.USER_KEY_PREFIX + memberId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(userKey));
    }

    /**
     * 큐에 사용자 추가
     */
    public void addToQueue(String memberId, WorldType worldType) {
        String queueKey = worldType.getQueueKey();
        String userKey = MatchingConstants.USER_KEY_PREFIX + memberId;

        // 큐 크기 확인
        Long currentSize = redisTemplate.opsForList().size(queueKey);
        if (currentSize != null && currentSize >= MatchingConstants.MAX_QUEUE_SIZE_PER_WORLD) {
            throw new IllegalStateException("매칭 대기열이 가득 찼습니다. 잠시 후 다시 시도해주세요.");
        }

        // 큐에 사용자 추가 (FIFO를 위해 leftPush 사용)
        redisTemplate.opsForList().leftPush(queueKey, memberId);

        // 사용자 상태 저장
        Map<String, String> userInfo = Map.of(
                "worldType", worldType.name(),
                "status", MatchingStatus.WAITING.name(),
                "joinedAt", Instant.now().toString(),
                "sessionId", UUID.randomUUID().toString()
        );

        redisTemplate.opsForHash().putAll(userKey, userInfo);
        redisTemplate.expire(userKey, Duration.ofSeconds(MatchingConstants.USER_STATUS_TTL_SECONDS));

        // 통계 업데이트
        updateQueueStats(worldType, 1);

        log.info("사용자 큐 추가 완료: memberId={}, worldType={}, queueSize={}", 
                memberId, worldType, currentSize != null ? currentSize + 1 : 1);
    }

    /**
     * 큐에서 사용자 제거 (취소 시)
     */
    public boolean removeFromQueue(String memberId) {
        String userKey = MatchingConstants.USER_KEY_PREFIX + memberId;

        // 사용자 정보 조회
        Map<Object, Object> userInfo = redisTemplate.opsForHash().entries(userKey);
        if (userInfo.isEmpty()) {
            return false; // 이미 큐에 없음
        }

        String worldTypeName = (String) userInfo.get("worldType");
        WorldType worldType = WorldType.valueOf(worldTypeName);
        String queueKey = worldType.getQueueKey();

        // 큐에서 사용자 제거
        Long removed = redisTemplate.opsForList().remove(queueKey, 0, memberId);
        
        // 사용자 상태 삭제
        redisTemplate.delete(userKey);

        // 통계 업데이트
        if (removed != null && removed > 0) {
            updateQueueStats(worldType, -1);
            log.info("사용자 큐 제거 완료: memberId={}, worldType={}", memberId, worldType);
            return true;
        }

        return false;
    }

    /**
     * 매칭 가능한지 확인
     */
    public boolean canProcessMatching(WorldType worldType) {
        String queueKey = worldType.getQueueKey();
        Long queueSize = redisTemplate.opsForList().size(queueKey);
        return queueSize != null && queueSize >= MatchingConstants.REQUIRED_PARTICIPANTS;
    }

    /**
     * 큐에서 3명 추출 (원자성 보장)
     */
    public List<String> extractThreeUsers(WorldType worldType) {
        String queueKey = worldType.getQueueKey();

        // Lua 스크립트로 원자성 보장
        List<String> users = redisTemplate.execute(
                new DefaultRedisScript<>(EXTRACT_USERS_SCRIPT, List.class),
                Collections.singletonList(queueKey)
        );

        if (users != null && users.size() == 3) {
            // 사용자 상태를 MATCHED로 변경
            users.forEach(this::markUserAsMatched);

            // 통계 업데이트
            updateQueueStats(worldType, -3);

            log.info("매칭 완료: worldType={}, participants={}", worldType, users);
            return users;
        }

        return Collections.emptyList();
    }

    /**
     * 사용자 상태를 MATCHED로 변경
     */
    private void markUserAsMatched(String memberId) {
        String userKey = MatchingConstants.USER_KEY_PREFIX + memberId;
        redisTemplate.opsForHash().put(userKey, "status", MatchingStatus.MATCHED.name());
    }

    /**
     * 현재 큐 크기 조회
     */
    public int getQueueSize(WorldType worldType) {
        String queueKey = worldType.getQueueKey();
        Long size = redisTemplate.opsForList().size(queueKey);
        return size != null ? size.intValue() : 0;
    }

    /**
     * 사용자의 큐 내 위치 조회
     */
    public int getUserQueuePosition(String userId, WorldType worldType) {
        String queueKey = worldType.getQueueKey();
        List<String> queueUsers = redisTemplate.opsForList().range(queueKey, 0, -1);
        
        if (queueUsers != null) {
            // 큐는 leftPush로 추가하므로, 뒤에서부터 찾아야 함 (FIFO 순서)
            for (int i = queueUsers.size() - 1; i >= 0; i--) {
                if (userId.equals(queueUsers.get(i))) {
                    return queueUsers.size() - i; // 1부터 시작하는 위치
                }
            }
        }
        
        return -1; // 큐에 없음
    }

    /**
     * 사용자 매칭 정보 조회
     */
    public Map<Object, Object> getUserMatchingInfo(String memberId) {
        String userKey = MatchingConstants.USER_KEY_PREFIX + memberId;
        return redisTemplate.opsForHash().entries(userKey);
    }

    /**
     * 큐 통계 업데이트
     */
    private void updateQueueStats(WorldType worldType, int delta) {
        String statsKey = worldType.getStatsKey();
        
        redisTemplate.opsForHash().increment(statsKey, "currentWaiting", delta);
        redisTemplate.opsForHash().put(statsKey, "lastUpdated", 
                Instant.now().toString());
        
        // 통계 데이터도 TTL 설정 (1일)
        redisTemplate.expire(statsKey, Duration.ofDays(1));
    }

    /**
     * 세계관별 큐 통계 조회
     */
    public Map<Object, Object> getQueueStats(WorldType worldType) {
        String statsKey = worldType.getStatsKey();
        return redisTemplate.opsForHash().entries(statsKey);
    }

    /**
     * 매칭 완료된 사용자들의 상태 정리
     */
    public void cleanupMatchedUsers(List<String> memberIds) {
        for (String memberId : memberIds) {
            String userKey = MatchingConstants.USER_KEY_PREFIX + memberId;
            redisTemplate.delete(userKey);
        }
        
        log.info("매칭 완료 사용자 상태 정리 완료: memberIds={}", memberIds);
    }

    /**
     * 특정 세계관 큐 초기화 (개발/테스트용)
     */
    public void clearQueue(WorldType worldType) {
        String queueKey = worldType.getQueueKey();
        String statsKey = worldType.getStatsKey();
        
        Set<String> memberIds = null;
        try {
            // 리스트 형태로 저장된 큐에서 사용자 ID 조회
            List<String> queueMemberIds = redisTemplate.opsForList().range(queueKey, 0, -1);
            if (queueMemberIds != null && !queueMemberIds.isEmpty()) {
                memberIds = new LinkedHashSet<>(queueMemberIds);
                for (String memberId : memberIds) {
                    String userKey = MatchingConstants.USER_KEY_PREFIX + memberId;
                    redisTemplate.delete(userKey);
                }
                log.info("큐 내 사용자 상태 정리 완료: worldType={}, memberCount={}", worldType, memberIds.size());
            }
        } catch (QueryTimeoutException e) {
            log.error("큐 정리 중 타임아웃 발생: worldType={}", worldType, e);
            // 타임아웃 시 재시도나 부분 정리 전략 고려
            throw new MatchingException(ErrorCode.MATCHING_PROCESSING_ERROR, "큐 정리 타임아웃: " + e.getMessage());
        } catch (DataAccessException e) {
            log.error("큐 정리 중 Redis 액세스 오류 발생: worldType={}", worldType, e);
            // Redis 연결 문제 시 예외 전파
            throw new MatchingException(ErrorCode.MATCHING_PROCESSING_ERROR, "Redis 액세스 오류: " + e.getMessage());
        } catch (Exception e) {
            log.error("큐 정리 중 예상치 못한 오류 발생: worldType={}", worldType, e);
            // 기타 예외도 전파하여 호출자가 적절히 처리할 수 있도록 함
            throw new MatchingException(ErrorCode.MATCHING_PROCESSING_ERROR, "큐 정리 오류: " + e.getMessage());
        }
        
        // 큐와 통계 데이터 삭제
        redisTemplate.delete(queueKey);
        redisTemplate.delete(statsKey);
        
        log.info("큐 초기화 완료: worldType={}, cleanedMembers={}", worldType, 
                memberIds != null ? memberIds.size() : 0);
    }
}