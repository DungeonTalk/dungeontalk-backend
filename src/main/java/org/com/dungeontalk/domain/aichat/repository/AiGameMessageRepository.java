package org.com.dungeontalk.domain.aichat.repository;

import org.com.dungeontalk.domain.aichat.common.AiMessageType;
import org.com.dungeontalk.domain.aichat.entity.AiGameMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 게임 메시지 MongoDB 접근을 위한 Repository
 * 
 * 턴제 메시지 히스토리 관리 및 AI 컨텍스트 조회에 사용
 */
@Repository
public interface AiGameMessageRepository extends MongoRepository<AiGameMessage, String> {

    /**
     * 특정 AI 게임방의 모든 메시지 조회 (시간순 정렬)
     * @param aiGameRoomId AI 게임방 ID
     * @return 메시지 목록 (오래된 순)
     */
    List<AiGameMessage> findByAiGameRoomIdOrderByCreatedAtAsc(String aiGameRoomId);

    /**
     * 특정 AI 게임방의 최근 메시지 조회 (페이징)
     * @param aiGameRoomId AI 게임방 ID
     * @param pageable 페이징 정보
     * @return 최근 메시지 목록
     */
    List<AiGameMessage> findByAiGameRoomIdOrderByCreatedAtDesc(String aiGameRoomId, Pageable pageable);

    /**
     * 특정 턴의 모든 메시지 조회 (메시지 순서대로)
     * @param aiGameRoomId AI 게임방 ID
     * @param turnNumber 턴 번호
     * @return 해당 턴의 메시지 목록
     */
    List<AiGameMessage> findByAiGameRoomIdAndTurnNumberOrderByMessageOrder(String aiGameRoomId, int turnNumber);

    /**
     * 특정 턴 범위의 메시지 조회 (AI 컨텍스트용)
     * @param aiGameRoomId AI 게임방 ID
     * @param startTurn 시작 턴
     * @param endTurn 종료 턴
     * @return 해당 범위의 모든 메시지
     */
    List<AiGameMessage> findByAiGameRoomIdAndTurnNumberBetweenOrderByTurnNumberAscMessageOrderAsc(
            String aiGameRoomId, int startTurn, int endTurn);

    /**
     * 특정 타입의 메시지들 조회
     * @param aiGameRoomId AI 게임방 ID
     * @param messageType 메시지 타입
     * @return 해당 타입의 메시지 목록
     */
    List<AiGameMessage> findByAiGameRoomIdAndMessageType(String aiGameRoomId, AiMessageType messageType);

    /**
     * 특정 사용자가 보낸 메시지들 조회
     * @param aiGameRoomId AI 게임방 ID
     * @param senderId 발신자 ID
     * @return 해당 사용자의 메시지 목록
     */
    List<AiGameMessage> findByAiGameRoomIdAndSenderIdOrderByCreatedAtAsc(String aiGameRoomId, String senderId);

    /**
     * 최근 N개 턴의 메시지 조회 (AI 컨텍스트 제한용)
     * @param aiGameRoomId AI 게임방 ID
     * @param recentTurnCount 최근 몇 개 턴
     * @param currentTurn 현재 턴 번호
     * @return 최근 N개 턴의 메시지
     */
    @Query("{ 'aiGameRoomId': ?0, 'turnNumber': { $gte: ?2 } }")
    List<AiGameMessage> findRecentTurnsMessages(String aiGameRoomId, int recentTurnCount, int fromTurn);

    /**
     * 특정 턴에서 다음 메시지 순서 번호 조회
     * @param aiGameRoomId AI 게임방 ID
     * @param turnNumber 턴 번호
     * @return 다음 메시지 순서 번호
     */
    @Query(value = "{ 'aiGameRoomId': ?0, 'turnNumber': ?1 }", 
           fields = "{ 'messageOrder': 1 }")
    List<AiGameMessage> findMaxMessageOrderByTurn(String aiGameRoomId, int turnNumber);

    /**
     * AI 응답 시간 통계용 조회
     * @param aiGameRoomId AI 게임방 ID
     * @param messageType AI 메시지만
     * @return AI 메시지 목록 (응답시간 있는 것만)
     */
    @Query("{ 'aiGameRoomId': ?0, 'messageType': ?1, 'aiResponseTime': { $exists: true } }")
    List<AiGameMessage> findAiMessagesWithResponseTime(String aiGameRoomId, AiMessageType messageType);

    /**
     * 특정 시간 이후의 메시지 조회
     * @param aiGameRoomId AI 게임방 ID
     * @param afterTime 기준 시간
     * @return 해당 시간 이후 메시지
     */
    List<AiGameMessage> findByAiGameRoomIdAndCreatedAtAfter(String aiGameRoomId, LocalDateTime afterTime);

    /**
     * 게임 ID로 메시지 조회 (다중 게임방 검색용)
     * @param gameId 게임 ID
     * @return 해당 게임의 모든 AI 메시지
     */
    List<AiGameMessage> findByGameIdOrderByCreatedAtAsc(String gameId);

    /**
     * 특정 게임방의 메시지 개수 조회
     * @param aiGameRoomId AI 게임방 ID
     * @return 메시지 총 개수
     */
    long countByAiGameRoomId(String aiGameRoomId);

    /**
     * 특정 턴의 메시지 개수 조회
     * @param aiGameRoomId AI 게임방 ID
     * @param turnNumber 턴 번호
     * @return 해당 턴의 메시지 개수
     */
    long countByAiGameRoomIdAndTurnNumber(String aiGameRoomId, int turnNumber);

    /**
     * 특정 게임방에서 사용자별 메시지 개수 조회
     * @param aiGameRoomId AI 게임방 ID
     * @param senderId 사용자 ID
     * @return 해당 사용자의 메시지 개수
     */
    long countByAiGameRoomIdAndSenderId(String aiGameRoomId, String senderId);

    /**
     * 오래된 메시지 정리용 조회
     * @param cutoffTime 기준 시간
     * @return 정리 대상 메시지 목록
     */
    List<AiGameMessage> findByCreatedAtBefore(LocalDateTime cutoffTime);
}