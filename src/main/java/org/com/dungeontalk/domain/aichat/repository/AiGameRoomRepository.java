package org.com.dungeontalk.domain.aichat.repository;

import org.com.dungeontalk.domain.aichat.common.AiGameStatus;
import org.com.dungeontalk.domain.aichat.entity.AiGameRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * AI 게임방 MongoDB 접근을 위한 Repository
 * 
 * 기존 ChatRoomRepository와 동일한 Spring Data MongoDB 패턴 활용
 */
@Repository
public interface AiGameRoomRepository extends MongoRepository<AiGameRoom, String> {

    /**
     * 게임 ID로 AI 게임방 조회
     * @param gameId PostgreSQL Game 테이블의 ID
     * @return AI 게임방 정보
     */
    Optional<AiGameRoom> findByGameId(String gameId);

    /**
     * 특정 사용자가 참여 중인 AI 게임방들 조회
     * @param participantId 참여자 ID (Member 테이블의 ID)
     * @return 참여 중인 게임방 목록
     */
    List<AiGameRoom> findByParticipantsContaining(String participantId);

    /**
     * 입장 가능한 게임방 조회 (상태가 CREATED이고 정원 미달)
     * @return 입장 가능한 게임방 목록
     */
    @Query("{ 'status': 'CREATED', $expr: { $lt: [ { $size: '$participants' }, '$maxParticipants' ] } }")
    List<AiGameRoom> findAvailableRooms();

    /**
     * 입장 가능한 게임방 조회 (페이징 지원)
     * @param pageable 페이징 정보
     * @return 입장 가능한 게임방 목록 (페이징)
     */
    @Query("{ 'status': 'CREATED', $expr: { $lt: [ { $size: '$participants' }, '$maxParticipants' ] } }")
    Page<AiGameRoom> findAvailableRooms(Pageable pageable);

    /**
     * 특정 사용자가 참여 중인 AI 게임방들 조회 (페이징 지원)
     * @param participantId 참여자 ID (Member 테이블의 ID)
     * @param pageable 페이징 정보
     * @return 참여 중인 게임방 목록 (페이징)
     */
    Page<AiGameRoom> findByParticipantsContaining(String participantId, Pageable pageable);

    /**
     * 마지막 활동 시간이 특정 시간 이전인 비활성 게임방 조회
     * (자동 정리용)
     * @param cutoffTime 기준 시간
     * @return 비활성 게임방 목록
     */
    List<AiGameRoom> findByLastActivityBefore(LocalDateTime cutoffTime);
}