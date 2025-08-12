package org.com.dungeontalk.domain.aichat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.aichat.dto.request.AiGameRoomCreateRequest;
import org.com.dungeontalk.domain.aichat.dto.request.AiGameRoomJoinRequest;
import org.com.dungeontalk.domain.aichat.dto.request.AiGameMessageSendRequest;
import org.com.dungeontalk.domain.aichat.dto.response.AiGameRoomResponse;
import org.com.dungeontalk.domain.aichat.common.AiGameStatus;
import org.com.dungeontalk.domain.aichat.common.AiMessageType;
import org.com.dungeontalk.domain.room.common.RoomType;
import org.com.dungeontalk.domain.room.dto.UnifiedRoomRequest;
import org.com.dungeontalk.domain.room.dto.UnifiedRoomResponse;
import org.com.dungeontalk.domain.room.dto.UnifiedMessageRequest;
import org.com.dungeontalk.domain.room.service.RoomService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 게임룸 서비스 어댑터
 * 기존 AiGameRoomService를 RoomService 인터페이스에 맞게 래핑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiGameRoomServiceAdapter implements RoomService {

    private final AiGameRoomService aiGameRoomService;
    private final AiGameMessageService aiGameMessageService;

    @Override
    public RoomType getSupportedRoomType() {
        return RoomType.AI_GAME;
    }

    @Override
    public UnifiedRoomResponse createRoom(UnifiedRoomRequest request) {
        log.info("AI 게임룸 생성 요청 (어댑터): roomName={}, creatorId={}", 
                request.getRoomName(), request.getCreatorId());
        
        // 요청 유효성 검증
        if (!request.isAiGameRoom()) {
            throw new IllegalArgumentException("AI 게임룸이 아닌 요청입니다: " + request.getRoomType());
        }
        
        // UnifiedRoomRequest -> AiGameRoomCreateRequest 변환
        AiGameRoomCreateRequest aiRequest = new AiGameRoomCreateRequest();
        aiRequest.setGameId(request.getGameId());
        aiRequest.setRoomName(request.getRoomName());
        aiRequest.setDescription(request.getDescription());
        aiRequest.setMaxParticipants(request.getMaxParticipants() != null ? request.getMaxParticipants() : 3);
        aiRequest.setGameSettings(request.getGameSettings());
        aiRequest.setCreatorId(request.getCreatorId());
        
        // 기존 서비스 호출
        AiGameRoomResponse aiResponse = aiGameRoomService.createAiGameRoom(aiRequest);
        
        // 참여자들 추가 (생성자 외 추가 참여자가 있는 경우)
        if (request.getParticipantIds() != null && request.getParticipantIds().size() > 1) {
            for (String participantId : request.getParticipantIds()) {
                if (!participantId.equals(request.getCreatorId())) {
                    try {
                        AiGameRoomJoinRequest joinReq = new AiGameRoomJoinRequest();
                        joinReq.setAiGameRoomId(aiResponse.getId());
                        joinReq.setParticipantId(participantId);
                        joinReq.setParticipantNickname(participantId); // 기본값으로 ID 사용
                        aiGameRoomService.joinAiGameRoom(joinReq);
                    } catch (Exception e) {
                        log.warn("참여자 추가 실패: participantId={}, error={}", participantId, e.getMessage());
                    }
                }
            }
        }
        
        // AiGameRoomResponse -> UnifiedRoomResponse 변환
        UnifiedRoomResponse response = UnifiedRoomResponse.fromAiGameRoom(aiResponse);
        
        log.info("AI 게임룸 생성 완료 (어댑터): roomId={}", response.getRoomId());
        return response;
    }

    @Override
    public UnifiedRoomResponse getRoom(String roomId) {
        log.debug("AI 게임룸 조회 (어댑터): roomId={}", roomId);
        
        AiGameRoomResponse aiResponse = aiGameRoomService.getAiGameRoom(roomId);
        return UnifiedRoomResponse.fromAiGameRoom(aiResponse);
    }

    @Override
    public void deleteRoom(String roomId) {
        log.info("AI 게임룸 삭제 (어댑터): roomId={}", roomId);
        
        // AI 게임룸은 일반적으로 직접 삭제하지 않고 상태를 변경
        // 필요시 aiGameRoomService에 삭제 메서드 추가 후 호출
        throw new UnsupportedOperationException("AI 게임룸 삭제는 현재 지원되지 않습니다");
    }

    @Override
    public List<UnifiedRoomResponse> getAvailableRooms() {
        log.debug("입장 가능한 AI 게임룸 목록 조회 (어댑터)");
        
        List<AiGameRoomResponse> aiRooms = aiGameRoomService.getAvailableRooms();
        return aiRooms.stream()
                .map(UnifiedRoomResponse::fromAiGameRoom)
                .collect(Collectors.toList());
    }

    @Override
    public UnifiedRoomResponse joinRoom(String roomId, String memberId) {
        log.info("AI 게임룸 참여 (어댑터): roomId={}, memberId={}", roomId, memberId);
        
        AiGameRoomJoinRequest joinRequest = new AiGameRoomJoinRequest();
        joinRequest.setAiGameRoomId(roomId);
        joinRequest.setParticipantId(memberId);
        joinRequest.setParticipantNickname(memberId); // 기본값으로 ID 사용
        
        AiGameRoomResponse aiResponse = aiGameRoomService.joinAiGameRoom(joinRequest);
        return UnifiedRoomResponse.fromAiGameRoom(aiResponse);
    }

    @Override
    public UnifiedRoomResponse leaveRoom(String roomId, String memberId) {
        log.info("AI 게임룸 퇴장 (어댑터): roomId={}, memberId={}", roomId, memberId);
        
        aiGameRoomService.leaveAiGameRoom(roomId, memberId);
        
        // 퇴장 후 룸 상태 조회하여 반환
        AiGameRoomResponse aiResponse = aiGameRoomService.getAiGameRoom(roomId);
        return UnifiedRoomResponse.fromAiGameRoom(aiResponse);
    }

    @Override
    public List<UnifiedRoomResponse> getUserRooms(String memberId) {
        log.debug("사용자 참여 AI 게임룸 목록 조회 (어댑터): memberId={}", memberId);
        
        List<AiGameRoomResponse> userRooms = aiGameRoomService.getUserParticipatingRooms(memberId);
        return userRooms.stream()
                .map(UnifiedRoomResponse::fromAiGameRoom)
                .collect(Collectors.toList());
    }

    @Override
    public void processMessage(UnifiedMessageRequest request) {
        log.debug("AI 게임 메시지 처리 (어댑터): roomId={}, messageType={}", 
                request.getRoomId(), request.getMessageType());
        
        // 요청 유효성 검증
        if (!request.isAiGameMessage()) {
            throw new IllegalArgumentException("AI 게임 메시지가 아닙니다: " + request.getRoomType());
        }
        
        // UnifiedMessageRequest -> AiGameMessageSendRequest 변환
        AiGameMessageSendRequest aiRequest = new AiGameMessageSendRequest();
        aiRequest.setAiGameRoomId(request.getAiGameRoomId() != null ? request.getAiGameRoomId() : request.getRoomId());
        aiRequest.setSenderId(request.getSenderId());
        aiRequest.setContent(request.getContent());
        aiRequest.setMessageType(mapToAiMessageType(request.getMessageType()));
        aiRequest.setSenderNickname(request.getSenderId()); // 기본값으로 ID 사용
        
        // 기존 AI 메시지 서비스 호출
        try {
            aiGameMessageService.processMessage(aiRequest);
        } catch (Exception e) {
            log.error("AI 게임 메시지 처리 중 오류 발생: roomId={}, error={}", 
                    request.getRoomId(), e.getMessage(), e);
            throw new RuntimeException("AI 게임 메시지 처리 실패", e);
        }
    }

    @Override
    public void sendSystemMessage(String roomId, String message) {
        log.debug("AI 게임룸 시스템 메시지 전송 (어댑터): roomId={}", roomId);
        
        AiGameMessageSendRequest systemRequest = new AiGameMessageSendRequest();
        systemRequest.setAiGameRoomId(roomId);
        systemRequest.setSenderId("SYSTEM");
        systemRequest.setContent(message);
        systemRequest.setMessageType(AiMessageType.SYSTEM);
        systemRequest.setSenderNickname("SYSTEM");
        
        try {
            aiGameMessageService.processMessage(systemRequest);
        } catch (Exception e) {
            log.error("AI 게임 시스템 메시지 전송 중 오류 발생: roomId={}, error={}", 
                    roomId, e.getMessage(), e);
            throw new RuntimeException("AI 게임 시스템 메시지 전송 실패", e);
        }
    }

    @Override
    public boolean existsRoom(String roomId) {
        try {
            aiGameRoomService.getAiGameRoom(roomId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isRoomActive(String roomId) {
        try {
            AiGameRoomResponse room = aiGameRoomService.getAiGameRoom(roomId);
            return room.getStatus() == AiGameStatus.ACTIVE;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean canJoinRoom(String roomId, String memberId) {
        try {
            AiGameRoomResponse room = aiGameRoomService.getAiGameRoom(roomId);
            return room.getCurrentParticipantCount() < room.getMaxParticipants() &&
                   !room.getParticipants().contains(memberId);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int getCurrentParticipantCount(String roomId) {
        try {
            AiGameRoomResponse room = aiGameRoomService.getAiGameRoom(roomId);
            return room.getCurrentParticipantCount();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public int getMaxParticipantCount(String roomId) {
        try {
            AiGameRoomResponse room = aiGameRoomService.getAiGameRoom(roomId);
            return room.getMaxParticipants();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 통합 메시지 타입을 AI 메시지 타입으로 매핑
     */
    private AiMessageType mapToAiMessageType(
            org.com.dungeontalk.domain.room.common.UnifiedMessageType unifiedType) {
        
        switch (unifiedType) {
            case USER:
                return AiMessageType.USER;
            case SYSTEM:
                return AiMessageType.SYSTEM;
            case AI_RESPONSE:
                return AiMessageType.AI;
            case GAME_ACTION:
                return AiMessageType.USER; // GAME_ACTION이 없으므로 USER로 대체
            case GAME_STATE:
                return AiMessageType.SYSTEM;
            default:
                log.warn("지원하지 않는 메시지 타입: {}. USER 타입으로 대체", unifiedType);
                return AiMessageType.USER;
        }
    }
}