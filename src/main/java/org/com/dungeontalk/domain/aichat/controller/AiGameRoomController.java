package org.com.dungeontalk.domain.aichat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.aichat.dto.request.AiGameRoomCreateRequest;
import org.com.dungeontalk.domain.aichat.dto.request.AiGameRoomJoinRequest;
import org.com.dungeontalk.domain.aichat.dto.response.AiGameMessageResponse;
import org.com.dungeontalk.domain.aichat.dto.response.AiGameRoomResponse;
import org.com.dungeontalk.domain.aichat.service.AiGameMessageService;
import org.com.dungeontalk.domain.aichat.service.AiGameRoomService;
import org.com.dungeontalk.domain.aichat.service.AiGameStateService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.com.dungeontalk.global.rsData.RsData;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "AI 게임방", description = "AI 게임방 관련 API")
@Slf4j
@RestController
@RequestMapping("/v1/aichat")
@RequiredArgsConstructor
public class AiGameRoomController {

    private final AiGameRoomService aiGameRoomService;
    private final AiGameMessageService aiGameMessageService;
    private final AiGameStateService aiGameStateService;

    @Operation(summary = "AI 게임방 생성", description = "새로운 AI 게임방을 생성합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "AI 게임방 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/rooms")
    public RsData<AiGameRoomResponse> createRoom(@Valid @RequestBody AiGameRoomCreateRequest request) {
        log.info("AI 게임방 생성 요청: gameId={}, creator={}", request.getGameId(), request.getCreatorId());
        
        AiGameRoomResponse response = aiGameRoomService.createAiGameRoom(request);
        return RsData.of("200", "AI 게임방 생성 완료", response);
    }

    @Operation(summary = "AI 게임방 참여", description = "기존 AI 게임방에 참여합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "AI 게임방 참여 성공"),
        @ApiResponse(responseCode = "400", description = "게임방이 가득참 또는 잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "게임방을 찾을 수 없음")
    })
    @PostMapping("/rooms/join")
    public RsData<AiGameRoomResponse> joinRoom(@Valid @RequestBody AiGameRoomJoinRequest request) {
        log.info("AI 게임방 참여 요청: roomId={}, participant={}", 
                 request.getAiGameRoomId(), request.getParticipantId());
        
        AiGameRoomResponse response = aiGameRoomService.joinAiGameRoom(request);
        return RsData.of("200", "AI 게임방 참여 완료", response);
    }

    @Operation(summary = "AI 게임방 퇴장", description = "AI 게임방에서 퇴장합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "AI 게임방 퇴장 성공"),
        @ApiResponse(responseCode = "404", description = "게임방 또는 참여자를 찾을 수 없음")
    })
    @PostMapping("/rooms/{roomId}/leave")
    public RsData<String> leaveRoom(
            @Parameter(description = "게임방 ID", required = true) @PathVariable String roomId, 
            @Parameter(description = "참여자 ID", required = true) @RequestParam String participantId) {
        log.info("AI 게임방 퇴장 요청: roomId={}, participant={}", roomId, participantId);
        
        aiGameRoomService.leaveAiGameRoom(roomId, participantId);
        return RsData.of("200", "AI 게임방 퇴장 완료", null);
    }

    @Operation(summary = "AI 게임방 정보 조회", description = "특정 AI 게임방의 상세 정보를 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "게임방 조회 성공"),
        @ApiResponse(responseCode = "404", description = "게임방을 찾을 수 없음")
    })
    @GetMapping("/rooms/{roomId}")
    public RsData<AiGameRoomResponse> getRoom(
            @Parameter(description = "게임방 ID", required = true) @PathVariable String roomId) {
        log.debug("AI 게임방 조회 요청: roomId={}", roomId);
        
        AiGameRoomResponse response = aiGameRoomService.getAiGameRoom(roomId);
        return RsData.of("200", "AI 게임방 조회 성공", response);
    }

    @Operation(summary = "게임 ID로 AI 게임방 조회", description = "게임 ID를 이용하여 AI 게임방을 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "게임방 조회 성공"),
        @ApiResponse(responseCode = "404", description = "게임방을 찾을 수 없음")
    })
    @GetMapping("/rooms/by-game/{gameId}")
    public RsData<AiGameRoomResponse> getRoomByGameId(
            @Parameter(description = "게임 ID", required = true) @PathVariable String gameId) {
        log.debug("게임 ID로 AI 게임방 조회 요청: gameId={}", gameId);
        
        AiGameRoomResponse response = aiGameRoomService.getAiGameRoomByGameId(gameId);
        return RsData.of("200", "게임 ID로 AI 게임방 조회 성공", response);
    }

    @Operation(summary = "입장 가능한 AI 게임방 목록 조회", description = "입장 가능한 AI 게임방 목록을 페이징으로 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "게임방 목록 조회 성공")
    })
    @GetMapping("/rooms/available")
    public RsData<Page<AiGameRoomResponse>> getAvailableRooms(
            @Parameter(description = "페이지 정보") @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.debug("입장 가능한 AI 게임방 목록 조회 요청");
        
        Page<AiGameRoomResponse> rooms = aiGameRoomService.getAvailableRooms(pageable);
        return RsData.of("200", "입장 가능한 AI 게임방 목록 조회 성공", rooms);
    }

    @Operation(summary = "사용자 참여 게임방 목록 조회", description = "사용자가 참여중인 AI 게임방 목록을 페이징으로 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "참여 게임방 목록 조회 성공")
    })
    @GetMapping("/rooms/my-rooms")
    public RsData<Page<AiGameRoomResponse>> getMyRooms(
            @Parameter(description = "참가자 ID", required = true) @RequestParam String participantId,
            @Parameter(description = "페이지 정보") @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.debug("사용자 참여 AI 게임방 목록 조회 요청: participantId={}", participantId);
        
        Page<AiGameRoomResponse> rooms = aiGameRoomService.getUserParticipatingRooms(participantId, pageable);
        return RsData.of("200", "사용자 참여 AI 게임방 목록 조회 성공", rooms);
    }

    @Operation(summary = "게임방 메시지 히스토리 조회", description = "AI 게임방의 메시지 히스토리를 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "메시지 히스토리 조회 성공")
    })
    @GetMapping("/rooms/{roomId}/messages")
    public RsData<List<AiGameMessageResponse>> getMessageHistory(
            @Parameter(description = "게임방 ID", required = true) @PathVariable String roomId,
            @Parameter(description = "페이지 정보") @PageableDefault(size = 50) Pageable pageable) {
        log.debug("AI 게임방 메시지 히스토리 조회 요청: roomId={}", roomId);
        
        List<AiGameMessageResponse> messages = aiGameMessageService.getMessageHistory(roomId, pageable);
        return RsData.of("200", "AI 게임방 메시지 히스토리 조회 성공", messages);
    }

    @Operation(summary = "특정 턴 메시지 조회", description = "특정 턴의 메시지를 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "턴 메시지 조회 성공")
    })
    @GetMapping("/rooms/{roomId}/turns/{turnNumber}/messages")
    public RsData<List<AiGameMessageResponse>> getTurnMessages(
            @Parameter(description = "게임방 ID", required = true) @PathVariable String roomId,
            @Parameter(description = "턴 번호", required = true) @PathVariable int turnNumber) {
        log.debug("특정 턴 메시지 조회 요청: roomId={}, turn={}", roomId, turnNumber);
        
        List<AiGameMessageResponse> messages = aiGameMessageService.getTurnMessages(roomId, turnNumber);
        return RsData.of("200", "특정 턴 메시지 조회 성공", messages);
    }

    @Operation(summary = "게임 세션 시작", description = "AI 게임 세션을 시작합니다")
    @PostMapping("/rooms/{roomId}/start")
    public RsData<AiGameRoomResponse> startGameSession(@PathVariable String roomId) {
        log.info("AI 게임 세션 시작 요청: roomId={}", roomId);
        
        AiGameRoomResponse response = aiGameStateService.startGameSession(roomId);
        return RsData.of("200", "AI 게임 세션 시작 성공", response);
    }

    @Operation(summary = "게임 일시정지", description = "AI 게임을 일시정지합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "게임 일시정지 성공")
    })
    @PostMapping("/rooms/{roomId}/pause")
    public RsData<String> pauseGame(
            @Parameter(description = "게임방 ID", required = true) @PathVariable String roomId, 
            @Parameter(description = "일시정지 사유") @RequestParam(defaultValue = "사용자 요청") String reason) {
        log.info("AI 게임 일시정지 요청: roomId={}, reason={}", roomId, reason);
        
        aiGameStateService.pauseGame(roomId, reason);
        return RsData.of("200", "AI 게임 일시정지 성공", null);
    }

    @Operation(summary = "게임 재개", description = "일시정지된 AI 게임을 재개합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "게임 재개 성공")
    })
    @PostMapping("/rooms/{roomId}/resume")
    public RsData<String> resumeGame(
            @Parameter(description = "게임방 ID", required = true) @PathVariable String roomId) {
        log.info("AI 게임 재개 요청: roomId={}", roomId);
        
        aiGameStateService.resumeGame(roomId);
        return RsData.of("200", "AI 게임 재개 성공", null);
    }

    @Operation(summary = "게임 종료", description = "AI 게임을 종료합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "게임 종료 성공")
    })
    @PostMapping("/rooms/{roomId}/end")
    public RsData<String> endGame(
            @Parameter(description = "게임방 ID", required = true) @PathVariable String roomId) {
        log.info("AI 게임 종료 요청: roomId={}", roomId);
        
        aiGameStateService.endGame(roomId);
        return RsData.of("200", "AI 게임 종료 성공", null);
    }

    @Operation(summary = "게임 상태 확인", description = "AI 게임의 현재 상태를 확인합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "게임 상태 조회 성공")
    })
    @GetMapping("/rooms/{roomId}/status")
    public RsData<GameStatusResponse> getGameStatus(
            @Parameter(description = "게임방 ID", required = true) @PathVariable String roomId) {
        log.debug("AI 게임 상태 확인 요청: roomId={}", roomId);
        
        boolean sessionValid = aiGameStateService.isSessionValid(roomId);
        boolean aiProcessing = aiGameStateService.isAiProcessing(roomId);
        
        GameStatusResponse statusResponse = new GameStatusResponse(roomId, sessionValid, aiProcessing);
        return RsData.of("200", "AI 게임 상태 조회 성공", statusResponse);
    }

    @Schema(description = "게임 상태 응답")
    public static class GameStatusResponse {
        @Schema(description = "게임방 ID", example = "room-12345")
        private final String roomId;
        
        @Schema(description = "세션 유효 여부", example = "true")
        private final boolean sessionValid;
        
        @Schema(description = "AI 처리 중 여부", example = "false")
        private final boolean aiProcessing;

        public GameStatusResponse(String roomId, boolean sessionValid, boolean aiProcessing) {
            this.roomId = roomId;
            this.sessionValid = sessionValid;
            this.aiProcessing = aiProcessing;
        }

        public String getRoomId() { return roomId; }
        public boolean isSessionValid() { return sessionValid; }
        public boolean isAiProcessing() { return aiProcessing; }
    }
}