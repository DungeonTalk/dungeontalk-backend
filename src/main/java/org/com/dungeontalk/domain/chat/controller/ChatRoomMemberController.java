package org.com.dungeontalk.domain.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.chat.dto.MemberPresenceDto;
import org.com.dungeontalk.domain.chat.entity.ChatRoomMember;
import org.com.dungeontalk.domain.chat.service.ChatRoomMemberService;
import org.com.dungeontalk.domain.chat.service.ChatRoomService;
import org.com.dungeontalk.global.rsData.RsData;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/chat/members")
@RequiredArgsConstructor
@Validated
@Tag(name = "Chat Members", description = "채팅방 멤버(입장/퇴장/온라인 목록) API")
public class ChatRoomMemberController {

    private final ChatRoomService chatRoomService;
    private final ChatRoomMemberService chatRoomMemberService;

    @Operation(summary = "멤버 입장", description = "memberId를 roomId에 입장시킵니다. 정원 초과 시 409 반환")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "입장 성공"),
        @ApiResponse(responseCode = "409", description = "정원 초과"),
        @ApiResponse(responseCode = "404", description = "채팅방 없음")
    })
    @PostMapping("/{roomId}/join/{memberId}")
    public RsData<String> join(
        @PathVariable @NotBlank String roomId,
        @PathVariable @NotBlank String memberId) {
        boolean joined = chatRoomService.joinRoom(roomId, memberId);
        if (!joined) {
            return RsData.of("409", "정원 초과 또는 이미 참여 중", roomId);
        }
        return RsData.of("200", "멤버 입장 완료", null);
    }

    @Operation(summary = "멤버 퇴장", description = "memberId를 roomId에서 퇴장시킵니다. 이미 퇴장된 경우에도 200")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "퇴장 처리 완료"),
        @ApiResponse(responseCode = "404", description = "채팅방 없음")
    })
    @PostMapping("/{roomId}/leave/{memberId}")
    public RsData<String> leave(
        @PathVariable String roomId, @PathVariable String memberId) {
        chatRoomService.leaveRoom(roomId, memberId);
        return RsData.of("200", "멤버 퇴장 완료", null);
    }

//    @Operation(summary = "온라인 멤버 조회", description = "roomId에 현재 온라인인 멤버 목록을 반환합니다(엔티티 대신 DTO).")
//    @ApiResponses({
//        @ApiResponse(responseCode = "200", description = "조회 성공",
//            content = @Content(mediaType = "application/json",
//                schema = @Schema(implementation = MemberPresenceDto.class)))
//    })
//    @GetMapping("/{roomId}/online")
//    public RsData<List<ChatRoomMember>> getOnlineMembers(@PathVariable String roomId) {
//        List<ChatRoomMember> members = chatRoomMemberService.getOnlineMembers(roomId);
//        return RsData.of("200", "온라인 멤버 조회 성공", members);
//    }

    @Operation(summary = "온라인 멤버 조회",
        description = "roomId 기준 현재 온라인 멤버 목록을 반환합니다. 엔티티 대신 DTO를 반환합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = MemberPresenceDto.class))))
    })
    @GetMapping("/{roomId}/online")
    public RsData<List<MemberPresenceDto>> getOnlineMembers(@PathVariable @NotBlank String roomId) {
        return RsData.of("200", "온라인 멤버 조회 성공",
            chatRoomMemberService.getOnlineMemberPresences(roomId));
    }

}
