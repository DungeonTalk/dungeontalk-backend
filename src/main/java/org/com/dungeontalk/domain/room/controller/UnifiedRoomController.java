package org.com.dungeontalk.domain.room.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.room.dto.RoomMemberRequest;
import org.com.dungeontalk.domain.room.dto.UnifiedRoomRequest;
import org.com.dungeontalk.domain.room.dto.UnifiedRoomResponse;
import org.com.dungeontalk.domain.room.service.UnifiedRoomService;
import org.com.dungeontalk.domain.aichat.service.AiGameStateService;
import org.com.dungeontalk.domain.aichat.service.AiGameFlowService;
import org.com.dungeontalk.domain.aichat.dto.request.AiGenerateRequest;
import org.com.dungeontalk.global.rsData.RsData;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@Tag(name = "통합 룸 관리", description = "AI 게임룸과 플레이어 채팅룸 통합 관리 API")
@Slf4j
@RestController
@RequestMapping("/v1/rooms")
@RequiredArgsConstructor
public class UnifiedRoomController {

    private final UnifiedRoomService unifiedRoomService;
    private final AiGameStateService aiGameStateService;
    private final AiGameFlowService aiGameFlowService;

    @Operation(summary = "룸 생성", description = "새로운 AI 게임룸 또는 플레이어 채팅룸을 생성합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "룸 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping
    public RsData<UnifiedRoomResponse> createRoom(@Valid @RequestBody UnifiedRoomRequest request) {
        return unifiedRoomService.createRoom(request);
    }

    @Operation(summary = "룸 정보 조회", description = "특정 룸의 상세 정보를 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "룸 정보 조회 성공"),
        @ApiResponse(responseCode = "404", description = "룸을 찾을 수 없음")
    })
    @GetMapping("/{roomType}/{roomId}")
    public RsData<UnifiedRoomResponse> getRoom(
            @Parameter(description = "룸 타입 (ai-game, player-chat)", required = true, example = "ai-game") @PathVariable String roomType, 
            @Parameter(description = "룸 ID", required = true, example = "room-12345") @PathVariable String roomId) {
        return unifiedRoomService.getRoom(roomType, roomId);
    }

    @Operation(summary = "입장 가능한 룸 목록 조회", description = "입장 가능한 모든 룸을 타입별로 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "입장 가능한 룸 목록 조회 성공")
    })
    @GetMapping("/available")
    public RsData<Map<String, List<UnifiedRoomResponse>>> getAvailableRooms() {
        return unifiedRoomService.getAvailableRooms();
    }

    @Operation(summary = "룸 참여", description = "룸에 참여합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "룸 참여 성공"),
        @ApiResponse(responseCode = "400", description = "참여 불가 (가득차거나 잘못된 요청)"),
        @ApiResponse(responseCode = "404", description = "룸을 찾을 수 없음")
    })
    @PostMapping("/{roomType}/{roomId}/join")
    public RsData<UnifiedRoomResponse> joinRoom(
            @Parameter(description = "룸 타입", required = true) @PathVariable String roomType,
            @Parameter(description = "룸 ID", required = true) @PathVariable String roomId,
            @Valid @RequestBody RoomMemberRequest request) {
        return unifiedRoomService.joinRoom(roomType, roomId, request.getMemberId());
    }

    @Operation(summary = "룸 퇴장", description = "룸에서 퇴장합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "룸 퇴장 성공"),
        @ApiResponse(responseCode = "404", description = "룸 또는 참여자를 찾을 수 없음")
    })
    @PostMapping("/{roomType}/{roomId}/leave")
    public RsData<UnifiedRoomResponse> leaveRoom(
            @Parameter(description = "룸 타입", required = true) @PathVariable String roomType,
            @Parameter(description = "룸 ID", required = true) @PathVariable String roomId,
            @Valid @RequestBody RoomMemberRequest request) {
        return unifiedRoomService.leaveRoom(roomType, roomId, request.getMemberId());
    }

    @Operation(summary = "사용자 참여 룸 목록 조회", description = "사용자가 참여중인 룸 목록을 타입별로 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "사용자 참여 룸 목록 조회 성공")
    })
    @GetMapping("/user/{memberId}")
    public RsData<Map<String, List<UnifiedRoomResponse>>> getUserRooms(
            @Parameter(description = "회원 ID", required = true, example = "user-12345") @PathVariable String memberId) {
        return unifiedRoomService.getUserRooms(memberId);
    }

    @Operation(summary = "룸 삭제", description = "룸을 삭제합니다 (관리자용)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "룸 삭제 성공"),
        @ApiResponse(responseCode = "404", description = "룸을 찾을 수 없음")
    })
    @DeleteMapping("/{roomType}/{roomId}")
    public RsData<String> deleteRoom(
            @Parameter(description = "룸 타입", required = true) @PathVariable String roomType,
            @Parameter(description = "룸 ID", required = true) @PathVariable String roomId) {
        return unifiedRoomService.deleteRoom(roomType, roomId);
    }

    @Operation(summary = "팩토리 상태 조회", description = "룸 팩토리 상태 정보를 조회합니다 (디버깅용)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "팩토리 상태 조회 성공")
    })
    @GetMapping("/factory/status")
    public RsData<String> getFactoryStatus() {
        return unifiedRoomService.getFactoryStatus();
    }

    @Operation(summary = "AI 게임 세션 시작", description = "AI 게임 세션을 시작합니다 (AI 게임룸 전용)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "AI 게임 세션 시작 성공"),
        @ApiResponse(responseCode = "500", description = "세션 시작 실패")
    })
    @PostMapping("/ai/{roomId}/start")
    public RsData<UnifiedRoomResponse> startAiGameSession(
            @Parameter(description = "AI 게임룸 ID", required = true, example = "room-12345") @PathVariable String roomId) {
        log.info("AI 게임 세션 시작 요청: roomId={}", roomId);
        
        try {
            var aiResponse = aiGameStateService.startGameSession(roomId);
            var unifiedResponse = UnifiedRoomResponse.fromAiGameRoom(aiResponse);
            return RsData.of("200", "AI 게임 세션 시작 성공", unifiedResponse);
        } catch (Exception e) {
            log.error("AI 게임 세션 시작 실패: roomId={}, error={}", roomId, e.getMessage(), e);
            return RsData.of("500", "AI 게임 세션 시작 실패: " + e.getMessage(), null);
        }
    }

    @Operation(summary = "AI 응답 생성 요청", description = "AI 응답 생성을 요청합니다 (AI 게임룸 전용)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "AI 응답 생성 요청 성공"),
        @ApiResponse(responseCode = "500", description = "AI 응답 생성 실패")
    })
    @PostMapping("/ai/{roomId}/ai/generate")
    public RsData<String> generateAiResponse(
            @Parameter(description = "AI 게임룸 ID", required = true, example = "room-12345") @PathVariable String roomId, 
            @Valid @RequestBody AiGenerateRequest request) {
        log.info("AI 응답 생성 요청: roomId={}, gameId={}", roomId, request.getGameId());
        
        try {
            aiGameFlowService.processAiTurn(roomId, request);
            return RsData.of("200-1", "AI 응답 생성 요청 성공", "AI 응답이 생성 중입니다.");
        } catch (Exception e) {
            log.error("AI 응답 생성 실패: roomId={}, error={}", roomId, e.getMessage(), e);
            return RsData.of("500", "AI 응답 생성 실패: " + e.getMessage(), null);
        }
    }
}