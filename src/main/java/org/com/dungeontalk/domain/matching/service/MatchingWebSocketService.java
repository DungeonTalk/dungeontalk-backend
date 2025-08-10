package org.com.dungeontalk.domain.matching.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.matching.common.MatchingConstants;
import org.com.dungeontalk.domain.matching.common.WorldType;
import org.com.dungeontalk.domain.matching.dto.websocket.MatchingWebSocketMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 개별 사용자에게 큐 상태 업데이트 전송
     */
    public void sendQueueStatusUpdate(String userId, WorldType worldType, int currentPosition, 
                                    int totalInQueue, long waitingTime) {
        MatchingWebSocketMessage message = MatchingWebSocketMessage.queueStatusUpdate(
                userId, worldType, currentPosition, totalInQueue, waitingTime);

        String destination = MatchingConstants.WS_TOPIC_USER_STATUS + userId;
        messagingTemplate.convertAndSend(destination, message);

        log.debug("큐 상태 업데이트 전송: userId={}, destination={}", userId, destination);
    }

    /**
     * 매칭 완료된 모든 참가자에게 알림 전송
     */
    public void sendMatchingComplete(List<String> participants, WorldType worldType,
                                   String gameSessionId, String aiGameRoomId, String chatRoomId) {
        MatchingWebSocketMessage message = MatchingWebSocketMessage.matchingComplete(
                participants, worldType, gameSessionId, aiGameRoomId, chatRoomId);

        // 각 참가자에게 개별 전송
        for (String userId : participants) {
            String destination = MatchingConstants.WS_TOPIC_USER_STATUS + userId;
            messagingTemplate.convertAndSend(destination, message);
        }

        log.info("매칭 완료 알림 전송 완료: participants={}, gameSessionId={}", participants, gameSessionId);
    }

    /**
     * 매칭 취소 알림 전송
     */
    public void sendMatchingCancelled(String userId, WorldType worldType) {
        MatchingWebSocketMessage message = MatchingWebSocketMessage.matchingCancelled(userId, worldType);

        String destination = MatchingConstants.WS_TOPIC_USER_STATUS + userId;
        messagingTemplate.convertAndSend(destination, message);

        log.info("매칭 취소 알림 전송: userId={}", userId);
    }

    /**
     * 세계관별 큐 전체 통계 브로드캐스트
     */
    public void broadcastQueueStats(WorldType worldType, int currentWaiting) {
        // 세계관별 큐 통계를 구독한 모든 클라이언트에게 전송
        String destination = MatchingConstants.WS_TOPIC_QUEUE_STATS + worldType.name().toLowerCase();
        
        MatchingWebSocketMessage.QueueStatusData queueData = MatchingWebSocketMessage.QueueStatusData.builder()
                .currentPosition(0)
                .totalInQueue(currentWaiting)
                .waitingTimeSeconds(0)
                .estimatedMessage("")
                .build();

        MatchingWebSocketMessage message = MatchingWebSocketMessage.builder()
                .type(MatchingWebSocketMessage.MessageType.QUEUE_STATUS_UPDATE)
                .worldType(worldType)
                .data(queueData)
                .build();

        messagingTemplate.convertAndSend(destination, message);

        log.debug("큐 통계 브로드캐스트: worldType={}, currentWaiting={}", worldType, currentWaiting);
    }

    /**
     * 에러 메시지 전송
     */
    public void sendError(String userId, String errorMessage) {
        MatchingWebSocketMessage message = MatchingWebSocketMessage.error(userId, errorMessage);

        String destination = MatchingConstants.WS_TOPIC_USER_STATUS + userId;
        messagingTemplate.convertAndSend(destination, message);

        log.warn("에러 메시지 전송: userId={}, error={}", userId, errorMessage);
    }
}