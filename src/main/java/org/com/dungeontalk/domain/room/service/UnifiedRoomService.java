package org.com.dungeontalk.domain.room.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.room.common.RoomType;
import org.com.dungeontalk.domain.room.dto.UnifiedRoomRequest;
import org.com.dungeontalk.domain.room.dto.UnifiedRoomResponse;
import org.com.dungeontalk.global.rsData.RsData;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 통합 룸 서비스
 * 모든 룸 관련 비즈니스 로직을 담당하며, 컨트롤러의 복잡성을 제거
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedRoomService {

    private final RoomServiceFactory roomServiceFactory;

    /**
     * 새로운 룸을 생성합니다 (ResponseService 패턴)
     */
    public RsData<UnifiedRoomResponse> createRoom(UnifiedRoomRequest request) {
        log.info("룸 생성 요청: roomType={}, roomName={}, creatorId={}", 
                request.getRoomType(), request.getRoomName(), request.getCreatorId());
        
        try {
            // 요청 유효성 검증
            request.validateByRoomType();
            
            // 해당 타입의 서비스 가져오기
            RoomService roomService = roomServiceFactory.getService(request.getRoomType());
            
            // 룸 생성
            UnifiedRoomResponse response = roomService.createRoom(request);
            
            log.info("룸 생성 성공: roomId={}, roomType={}", 
                    response.getRoomId(), response.getRoomType());
            
            return RsData.of("200", "룸 생성 성공", response);
            
        } catch (IllegalArgumentException e) {
            log.warn("룸 생성 실패 - 잘못된 요청: {}", e.getMessage());
            return RsData.of("400", "잘못된 요청: " + e.getMessage(), null);
        } catch (Exception e) {
            log.error("룸 생성 중 오류 발생: roomType={}", request.getRoomType(), e);
            return RsData.of("500", "룸 생성 중 오류가 발생했습니다", null);
        }
    }

    /**
     * 룸 정보를 조회합니다 (ResponseService 패턴)
     */
    public RsData<UnifiedRoomResponse> getRoom(String roomType, String roomId) {
        log.debug("룸 조회 요청: roomType={}, roomId={}", roomType, roomId);
        
        try {
            RoomService roomService = roomServiceFactory.getService(roomType);
            UnifiedRoomResponse response = roomService.getRoom(roomId);
            
            log.debug("룸 조회 성공: roomId={}, roomType={}", roomId, roomType);
            return RsData.of("200", "룸 조회 성공", response);
            
        } catch (IllegalArgumentException e) {
            log.warn("룸 조회 실패 - 잘못된 요청: roomType={}, roomId={}, error={}", 
                    roomType, roomId, e.getMessage());
            return RsData.of("400", "잘못된 요청: " + e.getMessage(), null);
        } catch (Exception e) {
            log.error("룸 조회 중 오류 발생: roomType={}, roomId={}", roomType, roomId, e);
            return RsData.of("500", "룸 조회 중 오류가 발생했습니다", null);
        }
    }

    /**
     * 입장 가능한 룸 목록을 조회합니다 (ResponseService 패턴)
     */
    public RsData<Map<String, List<UnifiedRoomResponse>>> getAvailableRooms() {
        log.debug("입장 가능한 룸 목록 조회 요청");
        
        try {
            Map<String, List<UnifiedRoomResponse>> availableRooms = roomServiceFactory
                    .getSupportedRoomTypes()
                    .stream()
                    .collect(Collectors.toMap(
                            RoomType::getCode,
                            roomType -> {
                                try {
                                    RoomService service = roomServiceFactory.getService(roomType);
                                    return service.getAvailableRooms();
                                } catch (Exception e) {
                                    log.warn("룸 타입 {} 조회 중 오류: {}", roomType, e.getMessage());
                                    return List.of();
                                }
                            }
                    ));
            
            log.debug("입장 가능한 룸 목록 조회 성공: 총 {} 개 타입", availableRooms.size());
            return RsData.of("200", "입장 가능한 룸 목록 조회 성공", availableRooms);
            
        } catch (Exception e) {
            log.error("입장 가능한 룸 목록 조회 중 오류 발생", e);
            return RsData.of("500", "룸 목록 조회 중 오류가 발생했습니다", null);
        }
    }

    /**
     * 룸에 참여합니다 (ResponseService 패턴)
     */
    public RsData<UnifiedRoomResponse> joinRoom(String roomType, String roomId, String memberId) {
        log.info("룸 참여 요청: roomType={}, roomId={}, memberId={}", roomType, roomId, memberId);
        
        try {
            RoomService roomService = roomServiceFactory.getService(roomType);
            UnifiedRoomResponse response = roomService.joinRoom(roomId, memberId);
            
            log.info("룸 참여 성공: roomId={}, memberId={}", roomId, memberId);
            return RsData.of("200", "룸 참여 성공", response);
            
        } catch (IllegalArgumentException e) {
            log.warn("룸 참여 실패 - 잘못된 요청: roomId={}, memberId={}, error={}", 
                    roomId, memberId, e.getMessage());
            return RsData.of("400", "잘못된 요청: " + e.getMessage(), null);
        } catch (Exception e) {
            log.error("룸 참여 중 오류 발생: roomId={}, memberId={}", roomId, memberId, e);
            return RsData.of("500", "룸 참여 중 오류가 발생했습니다", null);
        }
    }

    /**
     * 룸에서 퇴장합니다 (ResponseService 패턴)
     */
    public RsData<UnifiedRoomResponse> leaveRoom(String roomType, String roomId, String memberId) {
        log.info("룸 퇴장 요청: roomType={}, roomId={}, memberId={}", roomType, roomId, memberId);
        
        try {
            RoomService roomService = roomServiceFactory.getService(roomType);
            UnifiedRoomResponse response = roomService.leaveRoom(roomId, memberId);
            
            log.info("룸 퇴장 성공: roomId={}, memberId={}", roomId, memberId);
            return RsData.of("200", "룸 퇴장 성공", response);
            
        } catch (IllegalArgumentException e) {
            log.warn("룸 퇴장 실패 - 잘못된 요청: roomId={}, memberId={}, error={}", 
                    roomId, memberId, e.getMessage());
            return RsData.of("400", "잘못된 요청: " + e.getMessage(), null);
        } catch (Exception e) {
            log.error("룸 퇴장 중 오류 발생: roomId={}, memberId={}", roomId, memberId, e);
            return RsData.of("500", "룸 퇴장 중 오류가 발생했습니다", null);
        }
    }

    /**
     * 사용자가 참여중인 룸 목록을 조회합니다 (ResponseService 패턴)
     */
    public RsData<Map<String, List<UnifiedRoomResponse>>> getUserRooms(String memberId) {
        log.debug("사용자 참여 룸 목록 조회 요청: memberId={}", memberId);
        
        try {
            Map<String, List<UnifiedRoomResponse>> userRooms = roomServiceFactory
                    .getSupportedRoomTypes()
                    .stream()
                    .collect(Collectors.toMap(
                            RoomType::getCode,
                            roomType -> {
                                try {
                                    RoomService service = roomServiceFactory.getService(roomType);
                                    return service.getUserRooms(memberId);
                                } catch (Exception e) {
                                    log.warn("사용자 {} 의 {} 룸 목록 조회 중 오류: {}", 
                                            memberId, roomType, e.getMessage());
                                    return List.of();
                                }
                            }
                    ));
            
            log.debug("사용자 참여 룸 목록 조회 성공: memberId={}", memberId);
            return RsData.of("200", "사용자 참여 룸 목록 조회 성공", userRooms);
            
        } catch (Exception e) {
            log.error("사용자 참여 룸 목록 조회 중 오류 발생: memberId={}", memberId, e);
            return RsData.of("500", "룸 목록 조회 중 오류가 발생했습니다", null);
        }
    }

    /**
     * 룸 삭제 (관리자용) - ResponseService 패턴
     */
    public RsData<String> deleteRoom(String roomType, String roomId) {
        log.info("룸 삭제 요청: roomType={}, roomId={}", roomType, roomId);
        
        try {
            RoomService roomService = roomServiceFactory.getService(roomType);
            roomService.deleteRoom(roomId);
            
            log.info("룸 삭제 성공: roomId={}", roomId);
            return RsData.of("200", "룸 삭제 성공", "SUCCESS");
            
        } catch (IllegalArgumentException e) {
            log.warn("룸 삭제 실패 - 잘못된 요청: roomId={}, error={}", roomId, e.getMessage());
            return RsData.of("400", "잘못된 요청: " + e.getMessage(), null);
        } catch (Exception e) {
            log.error("룸 삭제 중 오류 발생: roomId={}", roomId, e);
            return RsData.of("500", "룸 삭제 중 오류가 발생했습니다", null);
        }
    }

    /**
     * 팩토리 상태 정보 조회 (디버깅용) - ResponseService 패턴
     */
    public RsData<String> getFactoryStatus() {
        try {
            String status = roomServiceFactory.getFactoryStatus();
            log.debug("팩토리 상태 조회 성공");
            return RsData.of("200", "팩토리 상태 조회 성공", status);
        } catch (Exception e) {
            log.error("팩토리 상태 조회 중 오류 발생", e);
            return RsData.of("500", "팩토리 상태 조회 중 오류가 발생했습니다", null);
        }
    }
}