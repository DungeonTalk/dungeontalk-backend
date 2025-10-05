package org.com.dungeontalk.domain.chat.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.chat.common.MessageType;
import org.com.dungeontalk.domain.chat.common.Status;
import org.com.dungeontalk.domain.chat.dto.MemberPresenceDto;
import org.com.dungeontalk.domain.chat.entity.ChatRoomMember;
import org.com.dungeontalk.domain.chat.event.ChatPresenceEvent;
import org.com.dungeontalk.domain.chat.repository.ChatRoomMemberRepository;
import org.com.dungeontalk.domain.member.entity.Member;
import org.com.dungeontalk.domain.member.repository.MemberRepository;
import org.com.dungeontalk.global.exception.ErrorCode;
import org.com.dungeontalk.global.exception.customException.ChatException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatRoomMemberService {
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ApplicationEventPublisher events;
    private final MemberRepository memberRepository;        // 닉네임 매핑용

    /**
     * 입장 기록 (멱등)
     * - 최초면 생성(ONLINE), 존재하면 ONLINE으로 변경
     * - 상태가 실제로 바뀐 경우에만 이벤트 발행(JOIN)
     */
    @Transactional
    public boolean markOnline(String roomId, String memberId) {
        if (roomId == null || memberId == null) {
            throw new ChatException(ErrorCode.CHAT_INVALID_PAYLOAD, "roomId/memberId required");
        }

        Instant now = Instant.now();

        Optional<ChatRoomMember> opt = chatRoomMemberRepository.findByRoomIdAndMemberId(roomId, memberId);

        ChatRoomMember chatRoomMember = opt.orElse(ChatRoomMember.builder()
            .roomId(roomId)
            .memberId(memberId)
            .status(Status.ONLINE)
            .joinedAt(now)
            .build());

        boolean changeStatus = (opt.isEmpty() || chatRoomMember.getStatus() != Status.ONLINE);

        chatRoomMember.online(now);
        chatRoomMemberRepository.save(chatRoomMember);

        if (changeStatus) {
            events.publishEvent(ChatPresenceEvent.builder()
                .roomId(roomId)
                .memberId(memberId)
                .type(MessageType.JOIN)
                .occurredAt(now)
                .build());
        }

        return changeStatus;
    }

    /**
     * 퇴장 기록
     * - 존재할 때만 OFFLINE으로 변경
     * - 상태가 실제로 바뀐 경우에만 이벤트 발행(LEAVE)
     */
    @Transactional
    public boolean markOffline(String roomId, String memberId) {
        if (roomId == null || memberId == null) {
            throw new ChatException(ErrorCode.CHAT_INVALID_PAYLOAD, "roomId/memberId required");
        }

        Instant now = Instant.now();
        final boolean[] changed = {false};

        chatRoomMemberRepository.findByRoomIdAndMemberId(roomId, memberId)
            .ifPresent(chatRoomMember -> {
            if (chatRoomMember.getStatus() != Status.OFFLINE) {
                chatRoomMember.offline(now);
                chatRoomMemberRepository.save(chatRoomMember);
                changed[0] = true;
            }
        });

        if (changed[0]) {
            events.publishEvent(ChatPresenceEvent.builder()
                .roomId(roomId)
                .memberId(memberId)
                .type(MessageType.LEAVE)
                .occurredAt(now)
                .build());
        }

        return changed[0];
    }

    @Transactional(readOnly = true)
    public List<ChatRoomMember> getOnlineMembers(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            throw new ChatException(ErrorCode.CHAT_INVALID_PAYLOAD, "roomId required");
        }

        return chatRoomMemberRepository.findByRoomId(roomId)
            .stream()
            .filter(m -> m.getStatus() == Status.ONLINE)
            .toList();
    }

    // 컨트롤러가 바로 사용할 DTO 반환 메서드 (비즈니스/매핑 로직 서비스로 이동)
    @Transactional(readOnly = true)
    public List<MemberPresenceDto> getOnlineMemberPresences(String roomId) {
        List<ChatRoomMember> members = getOnlineMembers(roomId);

        // memberId -> nickname 매핑 (PostgreSQL)
        List<String> ids = members.stream()
            .map(ChatRoomMember::getMemberId)
            .distinct()
            .toList();

        Map<String, String> idToNick = memberRepository.findByIdIn(ids)
            .stream()
            .collect(Collectors.toMap(Member::getId, Member::getNickName));

        return members.stream()
            .map(m -> MemberPresenceDto.builder()
                .memberId(m.getMemberId())
                .nickname(idToNick.getOrDefault(m.getMemberId(), "알 수 없음"))
                .status(m.getStatus())
                .build())
            .toList();
    }
}
