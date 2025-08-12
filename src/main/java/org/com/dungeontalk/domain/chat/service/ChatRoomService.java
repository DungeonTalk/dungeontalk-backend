package org.com.dungeontalk.domain.chat.service;

import static org.com.dungeontalk.domain.chat.dto.ChatRoomDto.fromEntity;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.chat.common.Status;
import org.com.dungeontalk.domain.chat.dto.ChatRoomDto;
import org.com.dungeontalk.domain.chat.dto.PresenceBroadcastDto;
import org.com.dungeontalk.domain.chat.dto.PresenceMessageDto;
import org.com.dungeontalk.domain.chat.dto.request.ChatRoomCreateRequestDto;
import org.com.dungeontalk.domain.chat.entity.ChatRoom;
import org.com.dungeontalk.domain.chat.repository.ChatRoomRepository;
import org.com.dungeontalk.domain.chat.util.ChatRoomProperties;
import org.com.dungeontalk.domain.member.entity.Member;
import org.com.dungeontalk.domain.member.repository.MemberRepository;
import org.com.dungeontalk.global.redis.ChatRoomMemberManager;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberService chatRoomMemberService;  // MongoDB
    private final ChatRoomMemberManager chatRoomMemberManager;  // Redis
    private final SimpMessageSendingOperations messagingTemplate;
    private final MemberRepository memberRepository;
    private final ChatRoomProperties chatRoomProperties; // ✅ 주입

    // 채팅방 생성
    @Transactional
    public ChatRoomDto createRoom(ChatRoomCreateRequestDto req) {
        Long capacity = (req.getMaxCapacity() != null)
            ? req.getMaxCapacity()
            : chatRoomProperties.getDefaultMaxCapacity();

        ChatRoom chatRoom = ChatRoom.builder()
            .roomName(req.getRoomName())
            .mode(req.getMode())
            .maxCapacity(capacity)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        ChatRoom saved = chatRoomRepository.save(chatRoom);
        return fromEntity(saved);
    }

    // 채팅방 단일 조회
    @Transactional(readOnly = true)
    public ChatRoomDto getRoomById(String roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + roomId));

        return fromEntity(room);
    }

    // 채팅방 전체 조회
    @Transactional(readOnly = true)
    public List<ChatRoomDto> getAllRooms() {
        return chatRoomRepository.findAll().stream()
            .map(ChatRoomDto::fromEntity)
            .toList();
    }

    // 참여자 입장
    @Transactional
    public boolean joinRoom(String roomId, String memberId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("채팅방 없음: " + roomId));

        // ✅ 유효 정원 계산: null → default, 0/음수 → 무제한
        Long capacity = (room.getMaxCapacity() != null) ? room.getMaxCapacity() : chatRoomProperties.getDefaultMaxCapacity();
        long current = chatRoomMemberManager.getUserCount(roomId);

        boolean capacityLimited = (capacity != null) && (capacity > 0);
        if (capacityLimited && current >= capacity) {
            // 정원 초과
            broadcastPresence(roomId, "JOIN_IGNORED");
            return false;
        }

        String nickName = memberRepository.findById(memberId)
            .map(Member::getNickName)
            .orElse("알 수 없음");

        // 입장
        boolean added = chatRoomMemberManager.addUser(roomId, memberId, nickName);  // SADD 결과

        // 누적 참여자 동기화 (중복 방지)
        if (added) {
            chatRoomMemberService.markOnline(roomId, memberId); // → JOIN 시스템 메시지 저장/전송
        }

        // 단일 Presence 브로드캐스트
        broadcastPresence(roomId, added ? "JOIN" : "JOIN_IGNORED");

        return added;
    }

    // 참여자 퇴장
    @Transactional
    public boolean leaveRoom(String roomId, String memberId) {
        boolean removed = chatRoomMemberManager.removeUser(roomId, memberId); // SREM 기반

        if (removed) {
            chatRoomMemberService.markOffline(roomId, memberId); // → LEAVE 시스템 메시지 저장/전송
        }

        broadcastPresence(roomId, removed ? "LEAVE" : "LEAVE_IGNORED");

        return removed;
    }

    // Presence 하나로만 브로드캐스트 (정원/현재인원/목록/이벤트)
    private void broadcastPresence(String roomId, String eventType) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("채팅방 없음: " + roomId));

        // 유효 정원 계산: room.maxCapacity가 null이면 설정 기본값 사용.
        // 0 또는 음수 → '무제한'으로 해석하고, 페이로드에 0으로 전달(프론트에서 '-' 처리 권장)
        Long max = resolveEffectiveCapacity(room);

        Map<String, String> nickMap = Optional
            .ofNullable(chatRoomMemberManager.getOnlineNickMap(roomId))
            .orElse(Collections.emptyMap());

        List<PresenceMessageDto.MemberPresence> members = nickMap.entrySet().stream()
            .map(e -> PresenceMessageDto.MemberPresence.builder()
                .memberId(e.getKey())
                .nickname(e.getValue())
                .status(Status.ONLINE)
                .build())
            .toList();

        long count = chatRoomMemberManager.getUserCount(roomId);

        PresenceBroadcastDto payload = PresenceBroadcastDto.of(
            roomId, max, count, members, eventType
        );

        // 구독 경로 통일 (기존: /sub/chat/room/{roomId})
        messagingTemplate.convertAndSend("/sub/chat/room/" + roomId, payload);
    }

    // ✅ 정원 결정 로직 (null → 설정 기본값, ≤0 → 무제한=0)
    private Long resolveEffectiveCapacity(ChatRoom room) {
        Long capacity = room.getMaxCapacity();
        if (capacity == null) {
            capacity = chatRoomProperties.getDefaultMaxCapacity(); // yml에서 주입, 없으면 properties에서 3으로 기본
        }

        // cap이 여전히 null이거나 0/음수면 무제한 취급 → 0 반환
        if (capacity == null || capacity <= 0) {
            return 0L;
        }
        return capacity;
    }

    /**
     * 운영 중 방의 정원을 바꿔야 할 수도 있으니, 현재 접속자 수보다 작은 값으로는 못 줄이게 설정
     */
    @Transactional
    public ChatRoomDto updateRoomCapacity(String roomId, Integer newCap) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("채팅방 없음: " + roomId));

        long online = chatRoomMemberManager.getUserCount(roomId);
        boolean limited = (newCap != null) && (newCap > 0);
        if (limited && online > newCap) {
            throw new IllegalStateException("현재 접속자(" + online + ")보다 작은 정원(" + newCap + ")으로는 설정할 수 없습니다.");
        }

        room.updateChatRoom(room.getMaxCapacity(), room.getUpdatedAt());
        ChatRoom saved = chatRoomRepository.save(room);

        // 정원 변경도 Presence로 알려주면 UX 좋음
        broadcastPresence(roomId, "CAPACITY_UPDATED");
        return ChatRoomDto.fromEntity(saved);
    }

}
