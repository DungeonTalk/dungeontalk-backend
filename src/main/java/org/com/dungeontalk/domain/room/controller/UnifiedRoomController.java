package org.com.dungeontalk.domain.room.controller;

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

/**
 * 통합된 룸 컨트롤러
 * AI 게임룸과 플레이어 채팅룸을 통합 관리
 */
@Slf4j
@RestController
@RequestMapping("/v1/rooms")
@RequiredArgsConstructor
public class UnifiedRoomController {

    private final UnifiedRoomService unifiedRoomService;
    private final AiGameStateService aiGameStateService;
    private final AiGameFlowService aiGameFlowService;

    /**
     * 새로운 룸을 생성합니다
     */
    @PostMapping
    public RsData<UnifiedRoomResponse> createRoom(@Valid @RequestBody UnifiedRoomRequest request) {
        return unifiedRoomService.createRoom(request);
    }

    /**
     * 룸 정보를 조회합니다
     */
    @GetMapping("/{roomType}/{roomId}")
    public RsData<UnifiedRoomResponse> getRoom(@PathVariable String roomType, 
                                             @PathVariable String roomId) {
        return unifiedRoomService.getRoom(roomType, roomId);
    }

    /**
     * 입장 가능한 룸 목록을 조회합니다
     */
    @GetMapping("/available")
    public RsData<Map<String, List<UnifiedRoomResponse>>> getAvailableRooms() {
        return unifiedRoomService.getAvailableRooms();
    }

    /**
     * 룸에 참여합니다
     */
    @PostMapping("/{roomType}/{roomId}/join")
    public RsData<UnifiedRoomResponse> joinRoom(@PathVariable String roomType,
                                              @PathVariable String roomId,
                                              @Valid @RequestBody RoomMemberRequest request) {
        return unifiedRoomService.joinRoom(roomType, roomId, request.getMemberId());
    }

    /**
     * 룸에서 퇴장합니다
     */
    @PostMapping("/{roomType}/{roomId}/leave")
    public RsData<UnifiedRoomResponse> leaveRoom(@PathVariable String roomType,
                                               @PathVariable String roomId,
                                               @Valid @RequestBody RoomMemberRequest request) {
        return unifiedRoomService.leaveRoom(roomType, roomId, request.getMemberId());
    }

    /**
     * 사용자가 참여중인 룸 목록을 조회합니다
     */
    @GetMapping("/user/{memberId}")
    public RsData<Map<String, List<UnifiedRoomResponse>>> getUserRooms(@PathVariable String memberId) {
        return unifiedRoomService.getUserRooms(memberId);
    }

    /**
     * 룸 삭제 (관리자용)
     */
    @DeleteMapping("/{roomType}/{roomId}")
    public RsData<String> deleteRoom(@PathVariable String roomType,
                                   @PathVariable String roomId) {
        return unifiedRoomService.deleteRoom(roomType, roomId);
    }

    /**
     * 팩토리 상태 정보 조회 (디버깅용)
     */
    @GetMapping("/factory/status")
    public RsData<String> getFactoryStatus() {
        return unifiedRoomService.getFactoryStatus();
    }

    /**
     * AI 게임 세션 시작 (AI 게임룸 전용)
     */
    @PostMapping("/ai/{roomId}/start")
    public RsData<UnifiedRoomResponse> startAiGameSession(@PathVariable String roomId) {
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

    /**
     * AI 응답 생성 요청 (AI 게임룸 전용)
     */
    @PostMapping("/ai/{roomId}/ai/generate")
    public RsData<String> generateAiResponse(@PathVariable String roomId, 
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