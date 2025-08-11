package org.com.dungeontalk.domain.chat.service;

import static org.com.dungeontalk.domain.chat.dto.ChatRoomDto.fromEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.chat.common.MessageType;
import org.com.dungeontalk.domain.chat.dto.ChatRoomDto;
import org.com.dungeontalk.domain.chat.dto.ConnectedCountMessageDto;
import org.com.dungeontalk.domain.chat.dto.PresenceMessageDto;
import org.com.dungeontalk.domain.chat.dto.request.ChatRoomCreateRequestDto;
import org.com.dungeontalk.domain.chat.entity.ChatRoom;
import org.com.dungeontalk.domain.chat.entity.ChatRoomMember;
import org.com.dungeontalk.domain.chat.repository.ChatRoomRepository;
import org.com.dungeontalk.domain.member.entity.Member;
import org.com.dungeontalk.domain.member.repository.MemberRepository;
import org.com.dungeontalk.global.redis.ChatRoomMemberManager;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberService chatRoomMemberService;  // MongoDB
    private final ChatRoomMemberManager chatRoomMemberManager;  // Redis
    private final SimpMessageSendingOperations messagingTemplate;
    private final MemberRepository memberRepository;

    private static final int MAX_ROOM_CAPACITY = 3;

    // 채팅방 생성
    public ChatRoomDto createRoom(ChatRoomCreateRequestDto req) {
        ChatRoom chatRoom = ChatRoom.builder()
            .roomType(req.getRoomType())
            .roomName(req.getRoomName())
            .mode(req.getMode())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        ChatRoom saved = chatRoomRepository.save(chatRoom);
        return fromEntity(saved);
    }

    // 채팅방 단일 조회
    public ChatRoomDto getRoomById(String roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + roomId));

        return fromEntity(room);
    }

    // 채팅방 전체 조회
    public List<ChatRoomDto> getAllRooms() {
        return chatRoomRepository.findAll().stream()
            .map(ChatRoomDto::fromEntity)
            .toList();
    }

    // 참여자 입장
    public void joinRoom(String roomId, String memberId) {
        // 인원 제한 (Redis 기준)
        long current = chatRoomMemberManager.getUserCount(roomId);
        if (current >= MAX_ROOM_CAPACITY) {
            throw new IllegalStateException("채팅방 인원이 가득 찼습니다.");
        }

        // 1. MongoDB 상태 ONLINE으로
        chatRoomMemberService.joinMember(roomId, memberId);

        // 2. Redis에 접속자 추가
        chatRoomMemberManager.addUser(roomId, memberId);

        // 3. 실시간 접속자 수 브로드캐스트
        broadcastConnectedCount(roomId);

        // 4. 현재 퇴장/재입장 상태 보여주기 - Presence 브로드캐스트
        broadcastPresence(roomId);
    }

    // 참여자 퇴장
    public void leaveRoom(String roomId, String memberId) {
        // 1. MongoDB 상태 OFFLINE으로
        chatRoomMemberService.leaveMember(roomId, memberId);

        // 2. Redis에서 접속자 제거
        chatRoomMemberManager.removeUser(roomId, memberId);

        // 3. 실시간 접속자 수 브로드캐스트
        long count = chatRoomMemberManager.getUserCount(roomId);
        ConnectedCountMessageDto msg = ConnectedCountMessageDto.builder()
            .roomId(roomId)
            .connectedCount(count)
            .type(MessageType.CONNECTED_COUNT)
            .build();
        messagingTemplate.convertAndSend("/sub/chat/room/" + roomId, msg);
    }

    // 접속자 수 파악
    private void broadcastConnectedCount(String roomId) {
        long count = chatRoomMemberManager.getUserCount(roomId);
        messagingTemplate.convertAndSend(
            "/sub/chat/room/" + roomId,
            ConnectedCountMessageDto.of(roomId, count)
        );
    }

    // 온라인에 접속 중인 멤버 조회
    private void broadcastPresence(String roomId) {
        List<ChatRoomMember> list = chatRoomMemberService.getOnlineMembers(roomId);
        List<String> ids = list.stream().map(ChatRoomMember::getMemberId).toList();

        Map<String,String> nickMap = memberRepository.findByIdIn(ids).stream()
            .collect(java.util.stream.Collectors.toMap(Member::getId, Member::getNickName));

        List<PresenceMessageDto.MemberPresence> members = list.stream()
            .map(m -> PresenceMessageDto.MemberPresence.builder()
                .memberId(m.getMemberId())
                .nickname(nickMap.getOrDefault(m.getMemberId(), "알 수 없음"))
                .status(m.getStatus())
                .build())
            .toList();

        long count = chatRoomMemberManager.getUserCount(roomId);
        messagingTemplate.convertAndSend(
            "/sub/chat/room/" + roomId,
            PresenceMessageDto.of(roomId, count, members)
        );
    }

}
