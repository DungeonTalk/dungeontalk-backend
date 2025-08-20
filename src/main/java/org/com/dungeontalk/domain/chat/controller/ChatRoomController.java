package org.com.dungeontalk.domain.chat.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
import org.springdoc.core.annotations.ParameterObject;
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
@RequestMapping("/v1/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    /**
     * 채팅방 생성
     */
    @Operation(summary = "채팅방 생성", description = "roomName/mode/maxCapacity로 방 생성")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "생성 성공",
            content = @Content(schema = @Schema(implementation = ChatRoomDto.class))),
        @ApiResponse(responseCode = "400", description = "유효성 실패")
    })
    @PostMapping("/room")
    public RsData<ChatRoomDto> createRoom(@Valid @RequestBody ChatRoomCreateRequestDto req) {
        ChatRoomDto createdRoom = chatRoomService.createRoom(req);
        return RsData.of("200", "채팅방 생성 완료", createdRoom);
    }

    /**
     * 채팅방 단일 조회
     */
    @Operation(summary = "채팅방 단일 조회", description = "roomId로 단일 채팅방 정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = ChatRoomDto.class))),
        @ApiResponse(responseCode = "404", description = "채팅방 없음")
    })
    @GetMapping("/room/{roomId}")
    public RsData<ChatRoomDto> getRoom(@PathVariable @NotBlank String roomId) {
        ChatRoomDto room = chatRoomService.getRoomById(roomId);
        return RsData.of("200", "채팅방 조회 성공", room);
    }

    /**
     * 채팅방 전체 조회
     */
    @Operation(summary = "채팅방 전체 조회", description = "모든 채팅방 목록을 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ChatRoomDto.class))))
    })
    @GetMapping("/room")
    public RsData<List<ChatRoomDto>> getAllRooms() {
        List<ChatRoomDto> rooms = chatRoomService.getAllRooms();
        return RsData.of("200", "전체 채팅방 조회 성공", rooms);
    }

    /**
     * 채팅방 입장
     */
    @Operation(summary = "채팅방 입장", description = "정원 초과 시 409 반환")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "입장 성공"),
        @ApiResponse(responseCode = "409", description = "정원 초과"),
        @ApiResponse(responseCode = "404", description = "채팅방 없음")
    })
    @PostMapping("/room/{roomId}/join/{memberId}")
    public RsData<String> joinRoom(
        @PathVariable @NotBlank String roomId,
        @PathVariable @NotBlank String memberId) {
        chatRoomService.joinRoom(roomId, memberId);
        return RsData.of("200", "채팅방 입장 성공", roomId);
    }

    /**
     * 채팅방 퇴장
     */
    @Operation(summary = "채팅방 퇴장", description = "이미 퇴장된 경우에도 200")
    @DeleteMapping("/room/{roomId}/leave/{memberId}")
    public RsData<String> leaveRoom(
        @PathVariable @NotBlank String roomId,
        @PathVariable @NotBlank String memberId) {
        chatRoomService.leaveRoom(roomId, memberId);
        return RsData.of("200", "채팅방 퇴장 성공", roomId);
    }

    /**
     * 채팅 메시지 전송 (STOMP + Redis Pub/Sub)
     */
    @Operation(summary = "메시지 전송",
        description = "STOMP + Redis Pub/Sub. TALK은 저장·브로드캐스트, JOIN/LEAVE는 Presence만 반영")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "전송 성공"),
        @ApiResponse(responseCode = "400", description = "roomId 불일치 또는 유효성 실패")
    })
    @PostMapping("/room/{roomId}/message")
    public RsData<ChatMessageDto> sendMessage(
        @PathVariable @NotBlank String roomId,
        @Valid @RequestBody ChatMessageSendRequestDto msg) throws JsonProcessingException {

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
    @Operation(summary = "메시지 목록 조회", description = "최신순 정렬(기본 createdAt desc)")
    @GetMapping("/room/{roomId}/messages")
    public RsData<Page<ChatMessageResponse>> getMessages(
        @PathVariable @NotBlank String roomId,
        @ParameterObject
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ChatMessageResponse> messages = chatMessageService.getMessagesByRoomId(roomId, pageable);
        return RsData.of("200", "채팅 메시지 목록 조회 성공", messages);
    }

}
