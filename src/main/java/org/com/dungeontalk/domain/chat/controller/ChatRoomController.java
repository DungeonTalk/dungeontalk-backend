package org.com.dungeontalk.domain.chat.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.chat.dto.ChatMessageDto;
import org.com.dungeontalk.domain.chat.dto.ChatRoomDto;
import org.com.dungeontalk.domain.chat.dto.request.ChatMessageSendRequestDto;
import org.com.dungeontalk.domain.chat.dto.request.ChatRoomCreateRequestDto;
import org.com.dungeontalk.domain.chat.dto.response.ChatMessageResponse;
import org.com.dungeontalk.domain.chat.service.ChatMessageService;
import org.com.dungeontalk.domain.chat.service.ChatRoomService;
import org.com.dungeontalk.global.rsData.RsData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    /**
     * 채팅방 생성
     */
    @PostMapping("/room")
    public RsData<ChatRoomDto> createRoom(@RequestBody ChatRoomCreateRequestDto req) {
        ChatRoomDto createdRoom = chatRoomService.createRoom(req);
        return RsData.of("200", "채팅방 생성 완료", createdRoom);
    }

    /**
     * 채팅방 단일 조회
     */
    @GetMapping("/room/{roomId}")
    public RsData<ChatRoomDto> getRoom(@PathVariable String roomId) {
        ChatRoomDto room = chatRoomService.getRoomById(roomId);
        return RsData.of("200", "채팅방 조회 성공", room);
    }

    /**
     * 채팅방 전체 조회
     */
    @GetMapping("/room")
    public RsData<List<ChatRoomDto>> getAllRooms() {
        List<ChatRoomDto> rooms = chatRoomService.getAllRooms();
        return RsData.of("200", "전체 채팅방 조회 성공", rooms);
    }

    /**
     * 채팅방 입장
     */
    @PostMapping("/room/{roomId}/join/{memberId}")
    public RsData<String> joinRoom(@PathVariable String roomId, @PathVariable String memberId) {
        chatRoomService.joinRoom(roomId, memberId);
        return RsData.of("200", "채팅방 입장 성공", null);
    }

    /**
     * 채팅방 퇴장
     */
    @DeleteMapping("/room/{roomId}/leave/{memberId}")
    public RsData<String> leaveRoom(@PathVariable String roomId, @PathVariable String memberId) {
        chatRoomService.leaveRoom(roomId, memberId);
        return RsData.of("200", "채팅방 퇴장 성공", null);
    }


    /**
     * 채팅 메시지 전송 (STOMP + Redis Pub/Sub)
     */
    @PostMapping("/room/{roomId}/message")
    public RsData<ChatMessageDto> sendMessage(
        @PathVariable String roomId,
        @RequestBody ChatMessageSendRequestDto msg) throws JsonProcessingException {

        if (msg == null || msg.getRoomId() == null) {
            return RsData.of("400", "요청 본문이 비어 있거나 roomId가 누락되었습니다.", null);
        }

        if (!roomId.equals(msg.getRoomId())) {
            return RsData.of("400", "요청 경로의 roomId와 body의 roomId가 일치하지 않습니다.", null);
        }

        ChatMessageDto result = chatMessageService.processMessage(msg);
        return RsData.of("200", "메시지 전송 성공", result);
    }

    /**
     * 메시지 목록 조회 (페이징 + 최신순 정렬)
     */
    @GetMapping("/room/{roomId}/messages")
    public RsData<Page<ChatMessageResponse>> getMessages(
        @PathVariable String roomId,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ChatMessageResponse> messages = chatMessageService.getMessagesByRoomId(roomId, pageable);
        return RsData.of("200", "채팅 메시지 목록 조회 성공", messages);
    }

}
