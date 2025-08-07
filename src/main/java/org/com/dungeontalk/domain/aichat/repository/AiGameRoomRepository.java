package org.com.dungeontalk.domain.aichat.repository;

import org.com.dungeontalk.domain.aichat.common.AiGameStatus;
import org.com.dungeontalk.domain.aichat.entity.AiGameRoom;
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
     * 상태별 AI 게임방 조회
     * @param status 게임방 상태
     * @return 해당 상태의 게임방 목록
     */
    List<AiGameRoom> findByStatus(AiGameStatus status);

    /**
     * 입장 가능한 게임방 조회 (상태가 CREATED이고 정원 미달)
     * @return 입장 가능한 게임방 목록
     */
    @Query("{ 'status': 'CREATED', $expr: { $lt: [ { $size: '$participants' }, '$maxParticipants' ] } }")
    List<AiGameRoom> findAvailableRooms();

    /**
     * 특정 사용자가 특정 상태의 게임에 참여 중인지 확인
     * @param participantId 참여자 ID
     * @param status 게임 상태
     * @return 참여 중인 게임방 (있다면)
     */
    Optional<AiGameRoom> findByParticipantsContainingAndStatus(String participantId, AiGameStatus status);

    /**
     * 마지막 활동 시간이 특정 시간 이전인 비활성 게임방 조회
     * (자동 정리용)
     * @param cutoffTime 기준 시간
     * @return 비활성 게임방 목록
     */
    List<AiGameRoom> findByLastActivityBefore(LocalDateTime cutoffTime);

    /**
     * 생성 시간 기준으로 최신 게임방들 조회 (페이징 가능)
     * @return 최신 게임방 목록
     */
    List<AiGameRoom> findAllByOrderByCreatedAtDesc();

    /**
     * 특정 게임 상태이면서 특정 시간 이후에 생성된 게임방 조회
     * @param status 게임 상태
     * @param createdAfter 생성 시간 기준
     * @return 해당하는 게임방 목록
     */
    List<AiGameRoom> findByStatusAndCreatedAtAfter(AiGameStatus status, LocalDateTime createdAfter);

    /**
     * 게임방 이름으로 검색 (부분 일치)
     * @param roomName 검색할 게임방 이름
     * @return 일치하는 게임방 목록
     */
    List<AiGameRoom> findByRoomNameContainingIgnoreCase(String roomName);

    /**
     * 특정 참여자 수인 게임방들 조회
     * @param participantCount 참여자 수
     * @return 해당 참여자 수를 가진 게임방 목록
     */
    @Query("{ $expr: { $eq: [ { $size: '$participants' }, ?0 ] } }")
    List<AiGameRoom> findByParticipantCount(int participantCount);
}