package org.com.dungeontalk.domain.chat.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.chat.entity.ChatRoomMember;
import org.com.dungeontalk.domain.chat.service.ChatRoomMemberService;
import org.com.dungeontalk.domain.chat.service.ChatRoomService;
import org.com.dungeontalk.global.rsData.RsData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/chat/members")
@RequiredArgsConstructor
public class ChatRoomMemberController {

    private final ChatRoomService chatRoomService;
    private final ChatRoomMemberService chatRoomMemberService;

    @PostMapping("/{roomId}/join/{memberId}")
    public RsData<String> join(@PathVariable String roomId, @PathVariable String memberId) {
        chatRoomService.joinRoom(roomId, memberId);
        return RsData.of("200", "멤버 입장 완료", null);
    }

    @PostMapping("/{roomId}/leave/{memberId}")
    public RsData<String> leave(@PathVariable String roomId, @PathVariable String memberId) {
        chatRoomService.leaveRoom(roomId, memberId);
        return RsData.of("200", "멤버 퇴장 완료", null);
    }

    @GetMapping("/{roomId}/online")
    public RsData<List<ChatRoomMember>> getOnlineMembers(@PathVariable String roomId) {
        List<ChatRoomMember> members = chatRoomMemberService.getOnlineMembers(roomId);
        return RsData.of("200", "온라인 멤버 조회 성공", members);
    }

}
