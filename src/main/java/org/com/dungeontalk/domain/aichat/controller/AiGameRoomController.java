package org.com.dungeontalk.domain.aichat.controller;

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

@Slf4j
@RestController
@RequestMapping("/api/v1/aichat")
@RequiredArgsConstructor
public class AiGameRoomController {

    private final AiGameRoomService aiGameRoomService;
    private final AiGameMessageService aiGameMessageService;
    private final AiGameStateService aiGameStateService;

    /**
     * AI 게임방 생성
     */
    @PostMapping("/rooms")
    public RsData<AiGameRoomResponse> createRoom(@Valid @RequestBody AiGameRoomCreateRequest request) {
        log.info("AI 게임방 생성 요청: gameId={}, creator={}", request.getGameId(), request.getCreatorId());
        
        AiGameRoomResponse response = aiGameRoomService.createAiGameRoom(request);
        return RsData.of("200", "AI 게임방 생성 완료", response);
    }

    /**
     * AI 게임방 참여
     */
    @PostMapping("/rooms/join")
    public RsData<AiGameRoomResponse> joinRoom(@Valid @RequestBody AiGameRoomJoinRequest request) {
        log.info("AI 게임방 참여 요청: roomId={}, participant={}", 
                 request.getAiGameRoomId(), request.getParticipantId());
        
        AiGameRoomResponse response = aiGameRoomService.joinAiGameRoom(request);
        return RsData.of("200", "AI 게임방 참여 완료", response);
    }

    /**
     * AI 게임방 퇴장
     */
    @PostMapping("/rooms/{roomId}/leave")
    public RsData<String> leaveRoom(@PathVariable String roomId, @RequestParam String participantId) {
        log.info("AI 게임방 퇴장 요청: roomId={}, participant={}", roomId, participantId);
        
        aiGameRoomService.leaveAiGameRoom(roomId, participantId);
        return RsData.of("200", "AI 게임방 퇴장 완료", null);
    }

    /**
     * AI 게임방 정보 조회
     */
    @GetMapping("/rooms/{roomId}")
    public RsData<AiGameRoomResponse> getRoom(@PathVariable String roomId) {
        log.debug("AI 게임방 조회 요청: roomId={}", roomId);
        
        AiGameRoomResponse response = aiGameRoomService.getAiGameRoom(roomId);
        return RsData.of("200", "AI 게임방 조회 성공", response);
    }

    /**
     * 게임 ID로 AI 게임방 조회
     */
    @GetMapping("/rooms/by-game/{gameId}")
    public RsData<AiGameRoomResponse> getRoomByGameId(@PathVariable String gameId) {
        log.debug("게임 ID로 AI 게임방 조회 요청: gameId={}", gameId);
        
        AiGameRoomResponse response = aiGameRoomService.getAiGameRoomByGameId(gameId);
        return RsData.of("200", "게임 ID로 AI 게임방 조회 성공", response);
    }

    /**
     * 입장 가능한 AI 게임방 목록 조회 (페이징 지원)
     */
    @GetMapping("/rooms/available")
    public RsData<Page<AiGameRoomResponse>> getAvailableRooms(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.debug("입장 가능한 AI 게임방 목록 조회 요청");
        
        Page<AiGameRoomResponse> rooms = aiGameRoomService.getAvailableRooms(pageable);
        return RsData.of("200", "입장 가능한 AI 게임방 목록 조회 성공", rooms);
    }

    /**
     * 사용자가 참여중인 AI 게임방 목록 조회 (페이징 지원)
     */
    @GetMapping("/rooms/my-rooms")
    public RsData<Page<AiGameRoomResponse>> getMyRooms(
            @RequestParam String participantId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.debug("사용자 참여 AI 게임방 목록 조회 요청: participantId={}", participantId);
        
        Page<AiGameRoomResponse> rooms = aiGameRoomService.getUserParticipatingRooms(participantId, pageable);
        return RsData.of("200", "사용자 참여 AI 게임방 목록 조회 성공", rooms);
    }

    /**
     * AI 게임방 메시지 히스토리 조회
     */
    @GetMapping("/rooms/{roomId}/messages")
    public RsData<List<AiGameMessageResponse>> getMessageHistory(
            @PathVariable String roomId,
            @PageableDefault(size = 50) Pageable pageable) {
        log.debug("AI 게임방 메시지 히스토리 조회 요청: roomId={}", roomId);
        
        List<AiGameMessageResponse> messages = aiGameMessageService.getMessageHistory(roomId, pageable);
        return RsData.of("200", "AI 게임방 메시지 히스토리 조회 성공", messages);
    }

    /**
     * 특정 턴의 메시지 조회
     */
    @GetMapping("/rooms/{roomId}/turns/{turnNumber}/messages")
    public RsData<List<AiGameMessageResponse>> getTurnMessages(
            @PathVariable String roomId,
            @PathVariable int turnNumber) {
        log.debug("특정 턴 메시지 조회 요청: roomId={}, turn={}", roomId, turnNumber);
        
        List<AiGameMessageResponse> messages = aiGameMessageService.getTurnMessages(roomId, turnNumber);
        return RsData.of("200", "특정 턴 메시지 조회 성공", messages);
    }

    /**
     * 게임 세션 시작
     */
    @PostMapping("/rooms/{roomId}/start")
    public RsData<AiGameRoomResponse> startGameSession(@PathVariable String roomId) {
        log.info("AI 게임 세션 시작 요청: roomId={}", roomId);
        
        AiGameRoomResponse response = aiGameStateService.startGameSession(roomId);
        return RsData.of("200", "AI 게임 세션 시작 성공", response);
    }

    /**
     * 게임 일시정지
     */
    @PostMapping("/rooms/{roomId}/pause")
    public RsData<String> pauseGame(@PathVariable String roomId, @RequestParam(defaultValue = "사용자 요청") String reason) {
        log.info("AI 게임 일시정지 요청: roomId={}, reason={}", roomId, reason);
        
        aiGameStateService.pauseGame(roomId, reason);
        return RsData.of("200", "AI 게임 일시정지 성공", null);
    }

    /**
     * 게임 재개
     */
    @PostMapping("/rooms/{roomId}/resume")
    public RsData<String> resumeGame(@PathVariable String roomId) {
        log.info("AI 게임 재개 요청: roomId={}", roomId);
        
        aiGameStateService.resumeGame(roomId);
        return RsData.of("200", "AI 게임 재개 성공", null);
    }

    /**
     * 게임 종료
     */
    @PostMapping("/rooms/{roomId}/end")
    public RsData<String> endGame(@PathVariable String roomId) {
        log.info("AI 게임 종료 요청: roomId={}", roomId);
        
        aiGameStateService.endGame(roomId);
        return RsData.of("200", "AI 게임 종료 성공", null);
    }

    /**
     * 게임 상태 확인
     */
    @GetMapping("/rooms/{roomId}/status")
    public RsData<GameStatusResponse> getGameStatus(@PathVariable String roomId) {
        log.debug("AI 게임 상태 확인 요청: roomId={}", roomId);
        
        boolean sessionValid = aiGameStateService.isSessionValid(roomId);
        boolean aiProcessing = aiGameStateService.isAiProcessing(roomId);
        
        GameStatusResponse statusResponse = new GameStatusResponse(roomId, sessionValid, aiProcessing);
        return RsData.of("200", "AI 게임 상태 조회 성공", statusResponse);
    }

    // Inner class for game status response
    public static class GameStatusResponse {
        private final String roomId;
        private final boolean sessionValid;
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