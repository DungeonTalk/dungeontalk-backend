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
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<AiGameRoomResponse> createRoom(@Valid @RequestBody AiGameRoomCreateRequest request) {
        log.info("AI 게임방 생성 요청: gameId={}, creator={}", request.getGameId(), request.getCreatorId());
        
        AiGameRoomResponse response = aiGameRoomService.createAiGameRoom(request);
        return ResponseEntity.ok(response);
    }

    /**
     * AI 게임방 참여
     */
    @PostMapping("/rooms/join")
    public ResponseEntity<AiGameRoomResponse> joinRoom(@Valid @RequestBody AiGameRoomJoinRequest request) {
        log.info("AI 게임방 참여 요청: roomId={}, participant={}", 
                 request.getAiGameRoomId(), request.getParticipantId());
        
        AiGameRoomResponse response = aiGameRoomService.joinAiGameRoom(request);
        return ResponseEntity.ok(response);
    }

    /**
     * AI 게임방 퇴장
     */
    @PostMapping("/rooms/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(@PathVariable String roomId, @RequestParam String participantId) {
        log.info("AI 게임방 퇴장 요청: roomId={}, participant={}", roomId, participantId);
        
        aiGameRoomService.leaveAiGameRoom(roomId, participantId);
        return ResponseEntity.ok().build();
    }

    /**
     * AI 게임방 정보 조회
     */
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<AiGameRoomResponse> getRoom(@PathVariable String roomId) {
        log.debug("AI 게임방 조회 요청: roomId={}", roomId);
        
        AiGameRoomResponse response = aiGameRoomService.getAiGameRoom(roomId);
        return ResponseEntity.ok(response);
    }

    /**
     * 게임 ID로 AI 게임방 조회
     */
    @GetMapping("/rooms/by-game/{gameId}")
    public ResponseEntity<AiGameRoomResponse> getRoomByGameId(@PathVariable String gameId) {
        log.debug("게임 ID로 AI 게임방 조회 요청: gameId={}", gameId);
        
        AiGameRoomResponse response = aiGameRoomService.getAiGameRoomByGameId(gameId);
        return ResponseEntity.ok(response);
    }

    /**
     * 입장 가능한 AI 게임방 목록 조회
     */
    @GetMapping("/rooms/available")
    public ResponseEntity<List<AiGameRoomResponse>> getAvailableRooms() {
        log.debug("입장 가능한 AI 게임방 목록 조회 요청");
        
        List<AiGameRoomResponse> rooms = aiGameRoomService.getAvailableRooms();
        return ResponseEntity.ok(rooms);
    }

    /**
     * 사용자가 참여중인 AI 게임방 목록 조회
     */
    @GetMapping("/rooms/my-rooms")
    public ResponseEntity<List<AiGameRoomResponse>> getMyRooms(@RequestParam String participantId) {
        log.debug("사용자 참여 AI 게임방 목록 조회 요청: participantId={}", participantId);
        
        List<AiGameRoomResponse> rooms = aiGameRoomService.getUserParticipatingRooms(participantId);
        return ResponseEntity.ok(rooms);
    }

    /**
     * AI 게임방 메시지 히스토리 조회
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<AiGameMessageResponse>> getMessageHistory(
            @PathVariable String roomId,
            @PageableDefault(size = 50) Pageable pageable) {
        log.debug("AI 게임방 메시지 히스토리 조회 요청: roomId={}", roomId);
        
        List<AiGameMessageResponse> messages = aiGameMessageService.getMessageHistory(roomId, pageable);
        return ResponseEntity.ok(messages);
    }

    /**
     * 특정 턴의 메시지 조회
     */
    @GetMapping("/rooms/{roomId}/turns/{turnNumber}/messages")
    public ResponseEntity<List<AiGameMessageResponse>> getTurnMessages(
            @PathVariable String roomId,
            @PathVariable int turnNumber) {
        log.debug("특정 턴 메시지 조회 요청: roomId={}, turn={}", roomId, turnNumber);
        
        List<AiGameMessageResponse> messages = aiGameMessageService.getTurnMessages(roomId, turnNumber);
        return ResponseEntity.ok(messages);
    }

    /**
     * 게임 세션 시작
     */
    @PostMapping("/rooms/{roomId}/start")
    public ResponseEntity<AiGameRoomResponse> startGameSession(@PathVariable String roomId) {
        log.info("AI 게임 세션 시작 요청: roomId={}", roomId);
        
        AiGameRoomResponse response = aiGameStateService.startGameSession(roomId);
        return ResponseEntity.ok(response);
    }

    /**
     * 게임 일시정지
     */
    @PostMapping("/rooms/{roomId}/pause")
    public ResponseEntity<Void> pauseGame(@PathVariable String roomId, @RequestParam(defaultValue = "사용자 요청") String reason) {
        log.info("AI 게임 일시정지 요청: roomId={}, reason={}", roomId, reason);
        
        aiGameStateService.pauseGame(roomId, reason);
        return ResponseEntity.ok().build();
    }

    /**
     * 게임 재개
     */
    @PostMapping("/rooms/{roomId}/resume")
    public ResponseEntity<Void> resumeGame(@PathVariable String roomId) {
        log.info("AI 게임 재개 요청: roomId={}", roomId);
        
        aiGameStateService.resumeGame(roomId);
        return ResponseEntity.ok().build();
    }

    /**
     * 게임 종료
     */
    @PostMapping("/rooms/{roomId}/end")
    public ResponseEntity<Void> endGame(@PathVariable String roomId) {
        log.info("AI 게임 종료 요청: roomId={}", roomId);
        
        aiGameStateService.endGame(roomId);
        return ResponseEntity.ok().build();
    }

    /**
     * 게임 상태 확인
     */
    @GetMapping("/rooms/{roomId}/status")
    public ResponseEntity<Object> getGameStatus(@PathVariable String roomId) {
        log.debug("AI 게임 상태 확인 요청: roomId={}", roomId);
        
        boolean sessionValid = aiGameStateService.isSessionValid(roomId);
        boolean aiProcessing = aiGameStateService.isAiProcessing(roomId);
        
        GameStatusResponse statusResponse = new GameStatusResponse(roomId, sessionValid, aiProcessing);
        return ResponseEntity.ok(statusResponse);
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