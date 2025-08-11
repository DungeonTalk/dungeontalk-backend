package org.com.dungeontalk.domain.chat.service;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.chat.common.Status;
import org.com.dungeontalk.domain.chat.entity.ChatRoomMember;
import org.com.dungeontalk.domain.chat.repository.ChatRoomMemberRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatRoomMemberService {
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public void joinMember(String roomId, String memberId) {
        ChatRoomMember member = chatRoomMemberRepository
            .findByRoomIdAndMemberId(roomId, memberId)
            .orElse(ChatRoomMember.builder()
                .roomId(roomId)
                .memberId(memberId)
                .status(Status.ONLINE)
                .joinedAt(Instant.now())
                .build());


        member.setStatus(Status.ONLINE);
        member.setUpdatedAt(Instant.now());
        chatRoomMemberRepository.save(member);
    }

    public void leaveMember(String roomId, String memberId) {
        chatRoomMemberRepository.findByRoomIdAndMemberId(roomId, memberId)
            .ifPresent(m -> {
                m.setStatus(Status.OFFLINE);
                m.setLeftAt(Instant.now());
                chatRoomMemberRepository.save(m);
            });
    }

    public List<ChatRoomMember> getOnlineMembers(String roomId) {
        return chatRoomMemberRepository.findByRoomId(roomId)
            .stream()
            .filter(m -> m.getStatus() == Status.ONLINE)
            .toList();
    }

    public long getOnlineCount(String roomId) {
        return chatRoomMemberRepository.countByRoomIdAndStatus(roomId, Status.ONLINE);
    }
}
