package org.com.dungeontalk.domain.room.service;

import org.com.dungeontalk.domain.room.dto.UnifiedRoomRequest;
import org.com.dungeontalk.domain.room.dto.UnifiedRoomResponse;
import org.com.dungeontalk.domain.room.dto.UnifiedMessageRequest;
import org.com.dungeontalk.domain.room.common.RoomType;

import java.util.List;

/**
 * 통합된 룸 서비스 인터페이스
 * AI 게임룸과 플레이어 채팅룸의 공통 기능을 정의
 */
public interface RoomService {

    /**
     * 이 서비스가 지원하는 룸 타입
     */
    RoomType getSupportedRoomType();

    // === 룸 관리 기능 ===

    /**
     * 새로운 룸을 생성합니다
     * 
     * @param request 룸 생성 요청 정보
     * @return 생성된 룸 정보
     */
    UnifiedRoomResponse createRoom(UnifiedRoomRequest request);

    /**
     * 룸 정보를 조회합니다
     * 
     * @param roomId 룸 ID
     * @return 룸 정보
     */
    UnifiedRoomResponse getRoom(String roomId);

    /**
     * 룸을 삭제합니다
     * 
     * @param roomId 룸 ID
     */
    void deleteRoom(String roomId);

    /**
     * 입장 가능한 룸 목록을 조회합니다
     * 
     * @return 입장 가능한 룸 목록
     */
    List<UnifiedRoomResponse> getAvailableRooms();

    // === 참여자 관리 기능 ===

    /**
     * 룸에 참여합니다
     * 
     * @param roomId 룸 ID
     * @param memberId 참여자 ID
     * @return 업데이트된 룸 정보
     */
    UnifiedRoomResponse joinRoom(String roomId, String memberId);

    /**
     * 룸에서 퇴장합니다
     * 
     * @param roomId 룸 ID
     * @param memberId 참여자 ID
     * @return 업데이트된 룸 정보
     */
    UnifiedRoomResponse leaveRoom(String roomId, String memberId);

    /**
     * 특정 사용자가 참여중인 룸 목록을 조회합니다
     * 
     * @param memberId 사용자 ID
     * @return 참여중인 룸 목록
     */
    List<UnifiedRoomResponse> getUserRooms(String memberId);

    // === 메시지 처리 기능 ===

    /**
     * 메시지를 처리합니다
     * 
     * @param request 메시지 요청 정보
     */
    void processMessage(UnifiedMessageRequest request);

    /**
     * 룸에 시스템 메시지를 전송합니다
     * 
     * @param roomId 룸 ID
     * @param message 메시지 내용
     */
    void sendSystemMessage(String roomId, String message);

    // === 상태 관리 기능 ===

    /**
     * 룸이 존재하는지 확인합니다
     * 
     * @param roomId 룸 ID
     * @return 존재 여부
     */
    boolean existsRoom(String roomId);

    /**
     * 룸이 활성 상태인지 확인합니다
     * 
     * @param roomId 룸 ID
     * @return 활성 상태 여부
     */
    boolean isRoomActive(String roomId);

    /**
     * 룸에 입장 가능한지 확인합니다
     * 
     * @param roomId 룸 ID
     * @param memberId 참여하려는 사용자 ID
     * @return 입장 가능 여부
     */
    boolean canJoinRoom(String roomId, String memberId);

    /**
     * 룸의 현재 참여자 수를 조회합니다
     * 
     * @param roomId 룸 ID
     * @return 현재 참여자 수
     */
    int getCurrentParticipantCount(String roomId);

    /**
     * 룸의 최대 참여자 수를 조회합니다
     * 
     * @param roomId 룸 ID
     * @return 최대 참여자 수
     */
    int getMaxParticipantCount(String roomId);
}