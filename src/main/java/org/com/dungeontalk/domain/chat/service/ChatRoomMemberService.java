package org.com.dungeontalk.domain.chat.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.chat.common.MessageType;
import org.com.dungeontalk.domain.chat.common.Status;
import org.com.dungeontalk.domain.chat.entity.ChatRoomMember;
import org.com.dungeontalk.domain.chat.event.ChatPresenceEvent;
import org.com.dungeontalk.domain.chat.repository.ChatRoomMemberRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatRoomMemberService {
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ApplicationEventPublisher events;

    /**
     * 입장 기록 (멱등)
     * - 최초면 생성(ONLINE), 존재하면 ONLINE으로 변경
     * - 상태가 실제로 바뀐 경우에만 이벤트 발행(JOIN)
     */
    @Transactional
    public boolean markOnline(String roomId, String memberId) {
        Instant now = Instant.now();

        Optional<ChatRoomMember> opt = chatRoomMemberRepository.findByRoomIdAndMemberId(roomId, memberId);
        ChatRoomMember chatRoomMember = opt.orElse(ChatRoomMember.builder()
            .roomId(roomId)
            .memberId(memberId)
            .status(Status.ONLINE)
            .joinedAt(now)
            .build());

        boolean changeStatus = (opt.isEmpty() || chatRoomMember.getStatus() != Status.ONLINE);

        chatRoomMember.setStatus(Status.ONLINE);
        chatRoomMember.setUpdatedAt(now);
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
        Instant now = Instant.now();
        final boolean[] changed = {false};

        chatRoomMemberRepository.findByRoomIdAndMemberId(roomId, memberId).ifPresent(chatRoomMember -> {
            if (chatRoomMember.getStatus() != Status.OFFLINE) {
                chatRoomMember.setStatus(Status.OFFLINE);
                chatRoomMember.setLeftAt(now);
                chatRoomMember.setUpdatedAt(now);
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
        return chatRoomMemberRepository.findByRoomId(roomId)
            .stream()
            .filter(m -> m.getStatus() == Status.ONLINE)
            .toList();
    }
}
