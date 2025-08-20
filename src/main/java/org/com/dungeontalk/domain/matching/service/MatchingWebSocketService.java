package org.com.dungeontalk.domain.matching.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.matching.common.MatchingConstants;
import org.com.dungeontalk.domain.worldtype.entity.WorldType;
import org.com.dungeontalk.domain.matching.dto.websocket.MatchingWebSocketMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final MatchingWebSocketMessageFactory messageFactory;

    /**
     * 개별 사용자에게 큐 상태 업데이트 전송
     */
    public void sendQueueStatusUpdate(String memberId, WorldType worldType, int currentPosition, 
                                    int totalInQueue, long waitingTime) {
        MatchingWebSocketMessage message = messageFactory.createQueueStatusUpdate(
                memberId, worldType, currentPosition, totalInQueue, waitingTime);

        String destination = MatchingConstants.WS_TOPIC_USER_STATUS + memberId;
        messagingTemplate.convertAndSend(destination, message);

        log.debug("큐 상태 업데이트 전송: memberId={}, destination={}", memberId, destination);
    }

    /**
     * 매칭 완료된 모든 참가자에게 알림 전송
     */
    public void sendMatchingComplete(List<String> participants, WorldType worldType,
                                   String gameSessionId, String aiGameRoomId, String chatRoomId) {
        MatchingWebSocketMessage message = messageFactory.createMatchingComplete(
                participants, worldType, gameSessionId, aiGameRoomId, chatRoomId);

        // 각 참가자에게 개별 전송
        for (String memberId : participants) {
            String destination = MatchingConstants.WS_TOPIC_USER_STATUS + memberId;
            messagingTemplate.convertAndSend(destination, message);
        }

        log.info("매칭 완료 알림 전송 완료: participants={}, gameSessionId={}", participants, gameSessionId);
    }

    /**
     * 매칭 취소 알림 전송
     */
    public void sendMatchingCancelled(String memberId, WorldType worldType) {
        MatchingWebSocketMessage message = messageFactory.createMatchingCancelled(memberId, worldType);

        String destination = MatchingConstants.WS_TOPIC_USER_STATUS + memberId;
        messagingTemplate.convertAndSend(destination, message);

        log.info("매칭 취소 알림 전송: memberId={}", memberId);
    }

    /**
     * 세계관별 큐 전체 통계 브로드캐스트
     */
    public void broadcastQueueStats(WorldType worldType, int currentWaiting) {
        // 세계관별 큐 통계를 구독한 모든 클라이언트에게 전송
        String destination = MatchingConstants.WS_TOPIC_QUEUE_STATS + worldType.getCode().toLowerCase();
        
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
    public void sendError(String memberId, String errorMessage) {
        MatchingWebSocketMessage message = messageFactory.createError(memberId, errorMessage);

        String destination = MatchingConstants.WS_TOPIC_USER_STATUS + memberId;
        messagingTemplate.convertAndSend(destination, message);

        log.warn("에러 메시지 전송: memberId={}, error={}", memberId, errorMessage);
    }
}