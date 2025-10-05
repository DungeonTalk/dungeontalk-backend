package org.com.dungeontalk.domain.matching.service;

import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.worldtype.entity.WorldType;
import org.com.dungeontalk.domain.matching.dto.websocket.MatchingWebSocketMessage;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * MatchingWebSocketMessage 생성을 담당하는 팩토리 클래스
 * DTO에서 비즈니스 로직을 분리하여 단일 책임 원칙을 준수
 */
@Slf4j
@Component
public class MatchingWebSocketMessageFactory {

    /**
     * 큐 상태 업데이트 메시지 생성
     */
    public MatchingWebSocketMessage createQueueStatusUpdate(String memberId, WorldType worldType, 
                                                          int currentPosition, int totalInQueue, 
                                                          long waitingTime) {
        MatchingWebSocketMessage.QueueStatusData data = MatchingWebSocketMessage.QueueStatusData.builder()
                .currentPosition(currentPosition)
                .totalInQueue(totalInQueue)
                .waitingTimeSeconds(waitingTime)
                .estimatedMessage(calculateEstimatedMessage(currentPosition))
                .build();

        return MatchingWebSocketMessage.builder()
                .type(MatchingWebSocketMessage.MessageType.QUEUE_STATUS_UPDATE)
                .memberId(memberId)
                .worldType(worldType)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 매칭 완료 메시지 생성
     */
    public MatchingWebSocketMessage createMatchingComplete(List<String> participants, WorldType worldType,
                                                         String gameSessionId, String aiGameRoomId, 
                                                         String chatRoomId) {
        MatchingWebSocketMessage.MatchingCompleteData data = MatchingWebSocketMessage.MatchingCompleteData.builder()
                .gameSessionId(gameSessionId)
                .aiGameRoomId(aiGameRoomId)
                .chatRoomId(chatRoomId)
                .participants(participants)
                .build();

        return MatchingWebSocketMessage.builder()
                .type(MatchingWebSocketMessage.MessageType.MATCHING_COMPLETE)
                .worldType(worldType)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 매칭 취소 메시지 생성
     */
    public MatchingWebSocketMessage createMatchingCancelled(String memberId, WorldType worldType) {
        MatchingWebSocketMessage.MatchingCancelData cancelData = MatchingWebSocketMessage.MatchingCancelData.builder()
                .memberId(memberId)
                .worldType(worldType)
                .message("매칭이 취소되었습니다")
                .build();
                
        return MatchingWebSocketMessage.builder()
                .type(MatchingWebSocketMessage.MessageType.MATCHING_CANCELLED)
                .memberId(memberId)
                .worldType(worldType)
                .data(cancelData)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 에러 메시지 생성
     */
    public MatchingWebSocketMessage createError(String memberId, String errorMessage) {
        return MatchingWebSocketMessage.builder()
                .type(MatchingWebSocketMessage.MessageType.ERROR)
                .memberId(memberId)
                .data(errorMessage)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 현재 위치를 기반으로 예상 대기 시간 메시지 계산
     */
    private String calculateEstimatedMessage(int position) {
        if (position <= 1) {
            return "곧 매칭될 예정입니다";
        } else if (position == 2) {
            return "약 30초 후 매칭 예정";
        } else {
            int estimatedMinutes = (position / 3) + 1;
            return String.format("약 %d분 후 매칭 예정", estimatedMinutes);
        }
    }
}