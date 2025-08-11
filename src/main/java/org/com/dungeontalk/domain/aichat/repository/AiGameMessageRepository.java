package org.com.dungeontalk.domain.aichat.repository;

import org.com.dungeontalk.domain.aichat.entity.AiGameMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AI 게임 메시지 MongoDB 접근을 위한 Repository
 * 
 * 핵심 기능: 턴제 메시지 히스토리 관리 및 AI 컨텍스트 조회
 */
@Repository
public interface AiGameMessageRepository extends MongoRepository<AiGameMessage, String> {

    /**
     * 특정 AI 게임방의 최근 메시지 조회 (페이징)
     * 💡 AI 컨텍스트 메시지 조회에 주로 사용
     */
    List<AiGameMessage> findByAiGameRoomIdOrderByCreatedAtDesc(String aiGameRoomId, Pageable pageable);

    /**
     * 특정 턴의 모든 메시지 조회 (메시지 순서대로)
     * 💡 턴별 메시지 히스토리 조회에 사용
     */
    @Query(value = "{ 'aiGameRoomId': ?0, 'turnNumber': ?1 }",
           sort = "{ 'messageOrder': 1 }")
    List<AiGameMessage> findTurnMessages(String aiGameRoomId, int turnNumber);

    /**
     * 최근 N개 턴의 메시지 조회 (AI 컨텍스트 제한용)
     * 💡 AI에게 제공할 컨텍스트 메시지 제한에 사용
     */
    @Query("{ 'aiGameRoomId': ?0, 'turnNumber': { $gte: ?2 } }")
    List<AiGameMessage> findRecentTurnsMessages(String aiGameRoomId, int recentTurnCount, int fromTurn);

    /**
     * 특정 턴에서 다음 메시지 순서 번호 조회
     * 💡 메시지 순서 자동 부여에 사용
     */
    @Query(value = "{ 'aiGameRoomId': ?0, 'turnNumber': ?1 }", 
           fields = "{ 'messageOrder': 1 }")
    List<AiGameMessage> findMaxMessageOrderByTurn(String aiGameRoomId, int turnNumber);
}