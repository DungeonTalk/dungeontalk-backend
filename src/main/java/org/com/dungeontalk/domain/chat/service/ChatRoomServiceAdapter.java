package org.com.dungeontalk.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.chat.dto.ChatRoomDto;
import org.com.dungeontalk.domain.chat.dto.request.ChatRoomCreateRequestDto;
import org.com.dungeontalk.domain.chat.dto.request.ChatMessageSendRequestDto;
import org.com.dungeontalk.domain.chat.common.ChatMode;
import org.com.dungeontalk.domain.chat.common.MessageType;
import org.com.dungeontalk.domain.room.common.RoomType;
import org.com.dungeontalk.domain.room.common.UnifiedMessageType;
import org.com.dungeontalk.domain.room.dto.UnifiedRoomRequest;
import org.com.dungeontalk.domain.room.dto.UnifiedRoomResponse;
import org.com.dungeontalk.domain.room.dto.UnifiedMessageRequest;
import org.com.dungeontalk.domain.room.service.RoomService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 채팅룸 서비스 어댑터
 * 기존 ChatRoomService를 RoomService 인터페이스에 맞게 래핑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomServiceAdapter implements RoomService {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    @Override
    public RoomType getSupportedRoomType() {
        return RoomType.PLAYER_CHAT;
    }

    @Override
    public UnifiedRoomResponse createRoom(UnifiedRoomRequest request) {
        log.info("플레이어 채팅룸 생성 요청 (어댑터): roomName={}, creatorId={}", 
                request.getRoomName(), request.getCreatorId());
        
        // 요청 유효성 검증
        if (!request.isPlayerChatRoom()) {
            throw new IllegalArgumentException("플레이어 채팅룸이 아닌 요청입니다: " + request.getRoomType());
        }
        
        // UnifiedRoomRequest -> ChatRoomCreateRequestDto 변환
        ChatRoomCreateRequestDto chatRequest = new ChatRoomCreateRequestDto();
        chatRequest.setRoomName(request.getRoomName());
        chatRequest.setMode(request.getChatMode() != null ? request.getChatMode() : ChatMode.MULTI);
        chatRequest.setMaxCapacity(request.getMaxCapacity());
        
        // 기존 서비스 호출
        ChatRoomDto chatResponse = chatRoomService.createRoom(chatRequest);
        
        // 참여자들 추가
        if (request.getParticipantIds() != null && !request.getParticipantIds().isEmpty()) {
            for (String participantId : request.getParticipantIds()) {
                try {
                    chatRoomService.joinRoom(chatResponse.getId(), participantId);
                } catch (Exception e) {
                    log.warn("참여자 추가 실패: participantId={}, error={}", participantId, e.getMessage());
                }
            }
        }
        
        // ChatRoomDto -> UnifiedRoomResponse 변환
        UnifiedRoomResponse response = UnifiedRoomResponse.fromPlayerChatRoom(chatResponse);
        
        log.info("플레이어 채팅룸 생성 완료 (어댑터): roomId={}", response.getRoomId());
        return response;
    }

    @Override
    public UnifiedRoomResponse getRoom(String roomId) {
        log.debug("플레이어 채팅룸 조회 (어댑터): roomId={}", roomId);
        
        ChatRoomDto chatResponse = chatRoomService.getRoomById(roomId);
        return UnifiedRoomResponse.fromPlayerChatRoom(chatResponse);
    }

    @Override
    public void deleteRoom(String roomId) {
        log.info("플레이어 채팅룸 삭제 (어댑터): roomId={}", roomId);
        
        // 채팅룸은 일반적으로 직접 삭제하지 않음
        // 필요시 chatRoomService에 삭제 메서드 추가 후 호출
        throw new UnsupportedOperationException("플레이어 채팅룸 삭제는 현재 지원되지 않습니다");
    }

    @Override
    public List<UnifiedRoomResponse> getAvailableRooms() {
        log.debug("입장 가능한 플레이어 채팅룸 목록 조회 (어댑터)");
        
        List<ChatRoomDto> chatRooms = chatRoomService.getAllRooms();
        return chatRooms.stream()
                .map(UnifiedRoomResponse::fromPlayerChatRoom)
                .collect(Collectors.toList());
    }

    @Override
    public UnifiedRoomResponse joinRoom(String roomId, String memberId) {
        log.info("플레이어 채팅룸 참여 (어댑터): roomId={}, memberId={}", roomId, memberId);
        
        boolean joined = chatRoomService.joinRoom(roomId, memberId);
        if (!joined) {
            throw new RuntimeException("채팅룸 참여에 실패했습니다. 정원이 초과되었거나 이미 참여중일 수 있습니다.");
        }
        
        // 참여 후 룸 상태 조회하여 반환
        ChatRoomDto chatResponse = chatRoomService.getRoomById(roomId);
        return UnifiedRoomResponse.fromPlayerChatRoom(chatResponse);
    }

    @Override
    public UnifiedRoomResponse leaveRoom(String roomId, String memberId) {
        log.info("플레이어 채팅룸 퇴장 (어댑터): roomId={}, memberId={}", roomId, memberId);
        
        boolean left = chatRoomService.leaveRoom(roomId, memberId);
        if (!left) {
            log.warn("채팅룸 퇴장 실패 또는 이미 퇴장된 상태: roomId={}, memberId={}", roomId, memberId);
        }
        
        // 퇴장 후 룸 상태 조회하여 반환
        ChatRoomDto chatResponse = chatRoomService.getRoomById(roomId);
        return UnifiedRoomResponse.fromPlayerChatRoom(chatResponse);
    }

    @Override
    public List<UnifiedRoomResponse> getUserRooms(String memberId) {
        log.debug("사용자 참여 플레이어 채팅룸 목록 조회 (어댑터): memberId={}", memberId);
        
        // 현재 ChatRoomService에 사용자별 룸 조회 메서드가 없으므로 전체 룸에서 필터링
        // 실제로는 Redis나 DB에서 사용자가 참여중인 룸만 조회하는 것이 효율적
        List<ChatRoomDto> allRooms = chatRoomService.getAllRooms();
        
        // TODO: 실제 구현에서는 사용자가 참여중인 룸만 필터링하는 로직 추가 필요
        return allRooms.stream()
                .map(UnifiedRoomResponse::fromPlayerChatRoom)
                .collect(Collectors.toList());
    }

    @Override
    public void processMessage(UnifiedMessageRequest request) {
        log.debug("플레이어 채팅 메시지 처리 (어댑터): roomId={}, messageType={}", 
                request.getRoomId(), request.getMessageType());
        
        // 요청 유효성 검증
        if (!request.isPlayerChatMessage()) {
            throw new IllegalArgumentException("플레이어 채팅 메시지가 아닙니다: " + request.getRoomType());
        }
        
        // UnifiedMessageRequest -> ChatMessageSendRequestDto 변환 (빌더 패턴 사용)
        ChatMessageSendRequestDto chatRequest = ChatMessageSendRequestDto.builder()
                .roomId(request.getChatRoomId() != null ? request.getChatRoomId() : request.getRoomId())
                .senderId(request.getSenderId())
                .content(request.getContent())
                .type(mapToChatMessageType(request.getMessageType()))
                .senderNickname(request.getSenderNickname() != null ? request.getSenderNickname() : request.getSenderId())
                .build();
        
        // 닉네임 설정 (있는 경우)
        if (request.getSenderNickname() != null) {
            // ChatMessageSendRequestDto에 nickname 필드가 있다면 설정
            // 현재 구조상 nickname은 별도 처리가 필요할 수 있음
        }
        
        // 기존 채팅 메시지 서비스 호출
        try {
            chatMessageService.processMessage(chatRequest);
        } catch (Exception e) {
            log.error("플레이어 채팅 메시지 처리 중 오류 발생: roomId={}, error={}", 
                    request.getRoomId(), e.getMessage(), e);
            throw new RuntimeException("플레이어 채팅 메시지 처리 실패", e);
        }
    }

    @Override
    public void sendSystemMessage(String roomId, String message) {
        log.debug("플레이어 채팅룸 시스템 메시지 전송 (어댑터): roomId={}", roomId);
        
        ChatMessageSendRequestDto systemRequest = ChatMessageSendRequestDto.builder()
                .roomId(roomId)
                .senderId("SYSTEM")
                .content(message)
                .type(MessageType.TALK) // SYSTEM이 없으므로 TALK으로 대체
                .senderNickname("SYSTEM")
                .build();
        
        try {
            chatMessageService.processMessage(systemRequest);
        } catch (Exception e) {
            log.error("플레이어 채팅 시스템 메시지 전송 중 오류 발생: roomId={}, error={}", 
                    roomId, e.getMessage(), e);
            throw new RuntimeException("플레이어 채팅 시스템 메시지 전송 실패", e);
        }
    }

    @Override
    public boolean existsRoom(String roomId) {
        try {
            chatRoomService.getRoomById(roomId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isRoomActive(String roomId) {
        try {
            ChatRoomDto room = chatRoomService.getRoomById(roomId);
            // 채팅룸은 생성되면 활성 상태로 간주
            return room != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean canJoinRoom(String roomId, String memberId) {
        try {
            ChatRoomDto room = chatRoomService.getRoomById(roomId);
            // 정원 체크는 chatRoomService.joinRoom()에서 처리되므로 여기서는 룸 존재 여부만 확인
            return room != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int getCurrentParticipantCount(String roomId) {
        try {
            // 현재 ChatRoomDto에 참여자 수 정보가 없으므로 0 반환
            // 실제로는 Redis에서 현재 온라인 사용자 수를 조회해야 함
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public int getMaxParticipantCount(String roomId) {
        try {
            ChatRoomDto room = chatRoomService.getRoomById(roomId);
            // ChatRoomDto에 getMaxCapacity() 메서드가 없으므로 기본값 반환
            return Integer.MAX_VALUE;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 통합 메시지 타입을 채팅 메시지 타입으로 매핑
     */
    private MessageType mapToChatMessageType(UnifiedMessageType unifiedType) {
        switch (unifiedType) {
            case USER:
                return MessageType.TALK;
            case SYSTEM:
                return MessageType.TALK; // SYSTEM이 없으므로 TALK으로 대체
            case OTHER_PLAYER:
                return MessageType.TALK;
            case PRESENCE:
                return MessageType.PRESENCE;
            case ROOM_INFO:
                return MessageType.TALK;
            default:
                log.warn("지원하지 않는 메시지 타입: {}. TALK 타입으로 대체", unifiedType);
                return MessageType.TALK;
        }
    }
}