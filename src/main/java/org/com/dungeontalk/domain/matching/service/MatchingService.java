package org.com.dungeontalk.domain.matching.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.aichat.dto.request.AiGameRoomCreateRequest;
import org.com.dungeontalk.domain.aichat.dto.response.AiGameRoomResponse;
import org.com.dungeontalk.domain.aichat.service.AiGameRoomService;
import org.com.dungeontalk.domain.chat.common.ChatMode;
import org.com.dungeontalk.domain.chat.common.ChatRoomType;
import org.com.dungeontalk.domain.chat.dto.ChatRoomDto;
import org.com.dungeontalk.domain.chat.dto.request.ChatRoomCreateRequestDto;
import org.com.dungeontalk.domain.chat.service.ChatRoomService;
import org.com.dungeontalk.domain.room.common.RoomType;
import org.com.dungeontalk.domain.room.dto.UnifiedRoomRequest;
import org.com.dungeontalk.domain.room.dto.UnifiedRoomResponse;
import org.com.dungeontalk.domain.room.service.RoomServiceFactory;
import org.com.dungeontalk.domain.matching.common.MatchingConstants;
import org.com.dungeontalk.domain.matching.common.MatchingStatus;
import org.com.dungeontalk.domain.matching.common.WorldType;
import org.com.dungeontalk.domain.matching.dto.response.MatchingCompleteResponse;
import org.com.dungeontalk.domain.matching.dto.response.MatchingStatusResponse;
import org.com.dungeontalk.domain.matching.dto.response.QueueStatsResponse;
import org.com.dungeontalk.domain.matching.dto.response.WorldQueueInfo;
import org.com.dungeontalk.domain.matching.exception.MatchingException;
import org.com.dungeontalk.global.exception.ErrorCode;
import org.com.dungeontalk.global.util.UuidV7Creator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.com.dungeontalk.domain.aichat.common.AiChatConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService {

    private final MatchingQueueManager queueManager;
    private final AiGameRoomService aiGameRoomService;
    private final ChatRoomService chatRoomService;
    private final StringRedisTemplate redisTemplate;
    private final MatchingWebSocketService webSocketService;
    private final RoomServiceFactory roomServiceFactory;

    /**
     * WebSocket 매칭 참가 처리 (컨트롤러 단순화용)
     */
    public void handleWebSocketJoinMatching(String memberId, WorldType worldType) {
        try {
            log.info("WebSocket 매칭 참가 요청: memberId={}, worldType={}", memberId, worldType);
            joinMatching(memberId, worldType);
            
        } catch (MatchingException e) {
            log.error("WebSocket 매칭 참가 중 매칭 오류: memberId={}, error={}", memberId, e.getMessage());
            webSocketService.sendError(memberId, e.getMessage());
        } catch (Exception e) {
            log.error("WebSocket 매칭 참가 중 예상치 못한 오류: memberId={}", memberId, e);
            webSocketService.sendError(memberId, "매칭 참가 중 오류가 발생했습니다.");
        }
    }

    /**
     * WebSocket 매칭 취소 처리 (컨트롤러 단순화용)
     */
    public void handleWebSocketCancelMatching(String memberId) {
        try {
            log.info("WebSocket 매칭 취소 요청: memberId={}", memberId);
            cancelMatching(memberId);
            
        } catch (Exception e) {
            log.error("WebSocket 매칭 취소 중 오류: memberId={}", memberId, e);
            webSocketService.sendError(memberId, "매칭 취소 중 오류가 발생했습니다.");
        }
    }

    /**
     * 매칭 큐 참가
     */
    public MatchingStatusResponse joinMatching(String memberId, WorldType worldType) {
        log.info("매칭 참가 요청: memberId={}, worldType={}", memberId, worldType);

        // 1. 중복 참가 체크
        if (queueManager.isUserInQueue(memberId)) {
            throw new MatchingException(ErrorCode.MATCHING_USER_ALREADY_IN_QUEUE);
        }

        try {
            // 2. 큐에 사용자 추가
            queueManager.addToQueue(memberId, worldType);

            // 3. 현재 상태 조회
            int queueSize = queueManager.getQueueSize(worldType);
            int userPosition = queueManager.getUserQueuePosition(memberId, worldType);

            // 4. WebSocket으로 상태 업데이트 전송
            webSocketService.sendQueueStatusUpdate(memberId, worldType, userPosition, queueSize, 0);
            
            // 5. 다른 대기 중인 사용자들에게도 업데이트된 큐 정보 전송
            notifyAllWaitingUsers(worldType, queueSize);

            // 6. 매칭 가능한지 확인하고 처리
            if (queueManager.canProcessMatching(worldType)) {
                // 비동기로 매칭 처리 (별도 스레드에서)
                processMatchingAsync(worldType);
            }

            return MatchingStatusResponse.of(
                    memberId, worldType, MatchingStatus.WAITING,
                    userPosition, queueSize, Instant.now()
            );

        } catch (Exception e) {
            log.error("매칭 참가 중 오류 발생: memberId={}, worldType={}", memberId, worldType, e);
            throw new MatchingException(ErrorCode.MATCHING_PROCESSING_ERROR, e.getMessage());
        }
    }

    /**
     * 매칭 취소
     */
    public boolean cancelMatching(String memberId) {
        log.info("매칭 취소 요청: memberId={}", memberId);

        // 사용자 정보 조회 (WebSocket 알림용)
        Map<Object, Object> userInfo = queueManager.getUserMatchingInfo(memberId);
        WorldType worldType = null;
        if (!userInfo.isEmpty()) {
            worldType = WorldType.valueOf((String) userInfo.get("worldType"));
        }

        boolean removed = queueManager.removeFromQueue(memberId);
        if (!removed) {
            log.warn("매칭 취소 실패: 사용자가 대기 중이 아님 - memberId={}", memberId);
            // 예외 대신 false 반환하도록 변경 (이미 취소된 상태일 수 있음)
            return false;
        }

        // WebSocket으로 취소 알림 전송 (worldType이 있을 때만)
        if (worldType != null) {
            try {
                webSocketService.sendMatchingCancelled(memberId, worldType);
                
                // 다른 대기 중인 사용자들에게도 업데이트된 큐 정보 전송
                int updatedQueueSize = queueManager.getQueueSize(worldType);
                notifyAllWaitingUsers(worldType, updatedQueueSize);
                
                log.info("매칭 취소 WebSocket 알림 전송 완료: memberId={}", memberId);
            } catch (Exception e) {
                log.warn("매칭 취소 WebSocket 알림 전송 실패: memberId={}, error={}", memberId, e.getMessage());
            }
        }

        return true;
    }

    /**
     * 사용자 매칭 상태 조회
     */
    public MatchingStatusResponse getMatchingStatus(String memberId) {
        Map<Object, Object> userInfo = queueManager.getUserMatchingInfo(memberId);
        
        if (userInfo.isEmpty()) {
            throw new MatchingException(ErrorCode.MATCHING_USER_NOT_IN_QUEUE);
        }

        String worldTypeName = (String) userInfo.get("worldType");
        String statusName = (String) userInfo.get("status");
        String joinedAtStr = (String) userInfo.get("joinedAt");

        WorldType worldType = WorldType.valueOf(worldTypeName);
        MatchingStatus status = MatchingStatus.valueOf(statusName);
        Instant joinedAt = Instant.parse(joinedAtStr);

        int queueSize = queueManager.getQueueSize(worldType);
        int userPosition = queueManager.getUserQueuePosition(memberId, worldType);

        return MatchingStatusResponse.of(memberId, worldType, status, userPosition, queueSize, joinedAt);
    }

    /**
     * 전체 큐 통계 조회
     */
    public QueueStatsResponse getQueueStats() {
        Map<WorldType, WorldQueueInfo> queueInfo = new HashMap<>();
        int totalWaiting = 0;

        for (WorldType worldType : WorldType.values()) {
            int currentWaiting = queueManager.getQueueSize(worldType);
            Map<Object, Object> stats = queueManager.getQueueStats(worldType);
            
            // 평균 대기 시간 (기본값: 30초)
            int averageWaitTime = 30;
            if (stats.containsKey("averageWaitTime")) {
                try {
                    averageWaitTime = Integer.parseInt((String) stats.get("averageWaitTime"));
                } catch (NumberFormatException e) {
                    log.warn("평균 대기 시간 파싱 실패, 기본값 사용: worldType={}", worldType);
                }
            }

            queueInfo.put(worldType, WorldQueueInfo.of(
                    worldType, currentWaiting, averageWaitTime
            ));

            totalWaiting += currentWaiting;
        }

        return QueueStatsResponse.builder()
                .queueInfo(queueInfo)
                .totalWaiting(totalWaiting)
                .lastUpdated(Instant.now().toString())
                .build();
    }

    /**
     * 매칭 처리 (비동기)
     */
    @Async("matchingTaskExecutor")
    public void processMatchingAsync(WorldType worldType) {
        try {
            processMatching(worldType);
        } catch (Exception e) {
            log.error("비동기 매칭 처리 중 오류: worldType={}", worldType, e);
        }
    }

    /**
     * 실제 매칭 처리 로직
     */
    public MatchingCompleteResponse processMatching(WorldType worldType) {
        log.info("매칭 처리 시작: worldType={}", worldType);
        
        String lockKey = MatchingConstants.LOCK_KEY_PREFIX + worldType.name();
        Boolean lockAcquired = false;
        
        try {
            // 분산 락 획득 (5초 타임아웃)
            lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "PROCESSING", Duration.ofSeconds(5));
            
            if (!Boolean.TRUE.equals(lockAcquired)) {
                log.warn("매칭 처리 락 획득 실패: worldType={} - 다른 프로세스에서 처리 중", worldType);
                return null;
            }
            
            log.debug("매칭 처리 락 획득 성공: worldType={}", worldType);

        try {
            // 1. 큐에서 3명 추출
            List<String> participants = queueManager.extractThreeUsers(worldType);
            if (participants.size() != 3) {
                log.warn("매칭 대상 부족: worldType={}, participants={}", worldType, participants.size());
                return null;
            }

            // 2. 게임 세션 ID 생성
            String gameSessionId = UuidV7Creator.create();

            // 3. 통합 룸 생성 (신규 방식)
            Map<String, String> roomIds = createUnifiedRooms(gameSessionId, participants, worldType);
            String aiGameRoomId = roomIds.get("ai");
            String chatRoomId = roomIds.get("chat");

            // 4. 매칭 세션 정보 Redis에 저장
            saveMatchingSession(gameSessionId, aiGameRoomId, chatRoomId, participants, worldType);

            // 5. WebSocket으로 매칭 완료 알림 전송
            webSocketService.sendMatchingComplete(participants, worldType, gameSessionId, 
                                                 aiGameRoomId, chatRoomId);

            // 6. 매칭 완료된 사용자들 상태 정리
            queueManager.cleanupMatchedUsers(participants);

            MatchingCompleteResponse response = MatchingCompleteResponse.of(
                    gameSessionId, aiGameRoomId, chatRoomId, worldType, participants
            );

            log.info("매칭 처리 완료: gameSessionId={}, participants={}", gameSessionId, participants);
            return response;

        } catch (Exception e) {
            log.error("매칭 처리 중 오류 발생: worldType={}", worldType, e);
            throw new MatchingException(ErrorCode.MATCHING_PROCESSING_ERROR, e.getMessage());
        }
        
        } finally {
            // 분산 락 해제
            if (Boolean.TRUE.equals(lockAcquired)) {
                redisTemplate.delete(lockKey);
                log.debug("매칭 처리 락 해제 완료: worldType={}", worldType);
            }
        }
    }

    /**
     * AI 게임방 생성
     * @deprecated 통합 룸 생성 메서드 createUnifiedRooms() 사용 권장
     */
    @Deprecated
    private AiGameRoomResponse createAiGameRoom(String gameSessionId, List<String> participants, WorldType worldType) {
        AiGameRoomCreateRequest request = new AiGameRoomCreateRequest();
        request.setGameId(gameSessionId);
        request.setRoomName(new StringBuilder()
                .append(worldType.getDisplayName())
                .append(" 랜덤 매칭")
                .toString());
        // description 필드 제거됨
        request.setMaxParticipants(DEFAULT_MAX_PARTICIPANTS);
        request.setGameSettings(worldType.getGameSettings());
        request.setCreatorId(participants.get(0)); // 첫 번째 사용자를 생성자로

        return aiGameRoomService.createAiGameRoom(request);
    }

    /**
     * 일반 채팅방 생성
     * @deprecated 통합 룸 생성 메서드 createUnifiedRooms() 사용 권장
     */
    @Deprecated
    private ChatRoomDto createChatRoom(String gameSessionId, List<String> participants, WorldType worldType) {
        ChatRoomCreateRequestDto request = new ChatRoomCreateRequestDto();
        request.setRoomName(new StringBuilder()
                .append(worldType.getDisplayName())
                .append(" 채팅방")
                .toString());
        request.setMode(ChatMode.MULTI);
        request.setParticipantIds(participants);

        return chatRoomService.createRoom(request);
    }

    /**
     * 매칭 세션 정보 Redis 저장
     */
    private void saveMatchingSession(String gameSessionId, String aiGameRoomId, String chatRoomId, 
                                   List<String> participants, WorldType worldType) {
        String sessionKey = MatchingConstants.SESSION_KEY_PREFIX + gameSessionId;

        Map<String, String> sessionInfo = Map.of(
                "participants", String.join(",", participants),
                "worldType", worldType.name(),
                "aiGameRoomId", aiGameRoomId,
                "chatRoomId", chatRoomId,
                "createdAt", Instant.now().toString(),
                "status", "ACTIVE"
        );

        redisTemplate.opsForHash().putAll(sessionKey, sessionInfo);
        redisTemplate.expire(sessionKey, Duration.ofSeconds(MatchingConstants.SESSION_INFO_TTL_SECONDS));

        log.info("매칭 세션 정보 저장 완료: gameSessionId={}", gameSessionId);
    }

    /**
     * 해당 세계관의 모든 대기 중인 사용자들에게 큐 상태 업데이트 전송
     */
    private void notifyAllWaitingUsers(WorldType worldType, int currentQueueSize) {
        try {
            // Redis에서 해당 세계관의 모든 대기 중인 사용자 조회
            String queueKey = worldType.getQueueKey();
            List<String> waitingUsers = redisTemplate.opsForList().range(queueKey, 0, -1);
            
            if (waitingUsers != null && !waitingUsers.isEmpty()) {
                // 각 사용자의 현재 위치 계산하여 개별 전송
                for (int i = 0; i < waitingUsers.size(); i++) {
                    String memberId = waitingUsers.get(i);
                    int position = waitingUsers.size() - i; // FIFO 순서 (leftPush, rightPop)
                    
                    webSocketService.sendQueueStatusUpdate(
                            memberId, worldType, position, currentQueueSize, 0);
                }
                
                log.debug("대기 중인 모든 사용자에게 큐 상태 업데이트 전송: worldType={}, userCount={}", 
                         worldType, waitingUsers.size());
            }
        } catch (Exception e) {
            log.warn("대기 사용자 알림 전송 실패: worldType={}, error={}", worldType, e.getMessage());
        }
    }

    /**
     * 정기적으로 매칭 가능한 큐 처리 (5초마다)
     */
    @Scheduled(fixedDelay = 5000)
    public void processAllQueueMatching() {
        for (WorldType worldType : WorldType.values()) {
            try {
                if (queueManager.canProcessMatching(worldType)) {
                    log.info("정기 매칭 처리 시작: worldType={}, queueSize={}", 
                            worldType, queueManager.getQueueSize(worldType));
                    processMatchingAsync(worldType);
                }
            } catch (Exception e) {
                log.error("정기 매칭 처리 중 오류: worldType={}", worldType, e);
            }
        }
    }

    // === 통합 룸 생성 메서드 (신규) ===

    /**
     * 통합된 룸 생성 메서드 (신규 - 중복 제거용)
     * AI 게임룸과 채팅룸을 한 번에 생성하고 ID 맵을 반환
     */
    public Map<String, String> createUnifiedRooms(String gameSessionId, List<String> participants, WorldType worldType) {
        log.info("통합 룸 생성 시작: gameSessionId={}, participants={}, worldType={}", gameSessionId, participants, worldType);
        
        Map<String, String> roomIds = new HashMap<>();
        
        try {
            // 1. AI 게임룸 생성 (통합 API 사용)
            UnifiedRoomRequest aiRoomRequest = UnifiedRoomRequest.builder()
                    .roomType(RoomType.AI_GAME)
                    .roomName(worldType.getDisplayName() + " 랜덤 매칭")
                    .maxParticipants(DEFAULT_MAX_PARTICIPANTS)
                    .creatorId(participants.get(0))
                    .participantIds(participants)
                    .gameId(gameSessionId)
                    .gameSettings(worldType.getGameSettings())
                    .build();
            
            UnifiedRoomResponse aiRoom = roomServiceFactory.getService(RoomType.AI_GAME).createRoom(aiRoomRequest);
            roomIds.put("ai", aiRoom.getRoomId());
            
            // 2. 플레이어 채팅룸 생성 (통합 API 사용)
            UnifiedRoomRequest chatRoomRequest = UnifiedRoomRequest.builder()
                    .roomType(RoomType.PLAYER_CHAT)
                    .roomName(worldType.getDisplayName() + " 채팅방")
                    .maxParticipants(DEFAULT_MAX_PARTICIPANTS)
                    .creatorId(participants.get(0))
                    .participantIds(participants)
                    .chatMode(ChatMode.MULTI)
                    .build();
            
            UnifiedRoomResponse chatRoom = roomServiceFactory.getService(RoomType.PLAYER_CHAT).createRoom(chatRoomRequest);
            roomIds.put("chat", chatRoom.getRoomId());
            
            log.info("통합 룸 생성 완료: aiRoomId={}, chatRoomId={}", roomIds.get("ai"), roomIds.get("chat"));
            
            return roomIds;
            
        } catch (Exception e) {
            log.error("통합 룸 생성 중 오류 발생: gameSessionId={}", gameSessionId, e);
            throw new MatchingException(ErrorCode.MATCHING_PROCESSING_ERROR, "룸 생성 실패: " + e.getMessage());
        }
    }

    /**
     * 기존 매칭 처리 로직에서 통합 룸 생성 사용 (기존 메서드 대체용)
     */
    public MatchingCompleteResponse processMatchingWithUnifiedRooms(WorldType worldType) {
        log.info("통합 룸 기반 매칭 처리 시작: worldType={}", worldType);
        
        String lockKey = MatchingConstants.LOCK_KEY_PREFIX + worldType.name();
        Boolean lockAcquired = false;
        
        try {
            // 분산 락 획득 (5초 타임아웃)
            lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "PROCESSING", Duration.ofSeconds(5));
            
            if (!Boolean.TRUE.equals(lockAcquired)) {
                log.warn("매칭 처리 락 획득 실패: worldType={} - 다른 프로세스에서 처리 중", worldType);
                return null;
            }
            
            log.debug("매칭 처리 락 획득 성공: worldType={}", worldType);

        try {
            // 1. 큐에서 3명 추출
            List<String> participants = queueManager.extractThreeUsers(worldType);
            if (participants.size() != 3) {
                log.warn("매칭 대상 부족: worldType={}, participants={}", worldType, participants.size());
                return null;
            }

            // 2. 게임 세션 ID 생성
            String gameSessionId = UuidV7Creator.create();

            // 3. 통합 룸 생성 (신규 메서드 사용)
            Map<String, String> roomIds = createUnifiedRooms(gameSessionId, participants, worldType);
            String aiGameRoomId = roomIds.get("ai");
            String chatRoomId = roomIds.get("chat");

            // 4. 매칭 세션 정보 Redis에 저장
            saveMatchingSession(gameSessionId, aiGameRoomId, chatRoomId, participants, worldType);

            // 5. WebSocket으로 매칭 완료 알림 전송
            webSocketService.sendMatchingComplete(participants, worldType, gameSessionId, 
                                                 aiGameRoomId, chatRoomId);

            // 6. 매칭 완료된 사용자들 상태 정리
            queueManager.cleanupMatchedUsers(participants);

            MatchingCompleteResponse response = MatchingCompleteResponse.of(
                    gameSessionId, aiGameRoomId, chatRoomId, worldType, participants
            );

            log.info("통합 룸 기반 매칭 처리 완료: gameSessionId={}, participants={}", gameSessionId, participants);
            return response;

        } catch (Exception e) {
            log.error("통합 룸 기반 매칭 처리 중 오류 발생: worldType={}", worldType, e);
            throw new MatchingException(ErrorCode.MATCHING_PROCESSING_ERROR, e.getMessage());
        }
        
        } finally {
            // 분산 락 해제
            if (Boolean.TRUE.equals(lockAcquired)) {
                redisTemplate.delete(lockKey);
                log.debug("매칭 처리 락 해제 완료: worldType={}", worldType);
            }
        }
    }
}