package org.com.dungeontalk.domain.aichat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.aichat.common.AiGamePhase;
import org.com.dungeontalk.domain.aichat.common.AiGameStatus;
import org.com.dungeontalk.domain.aichat.dto.SessionDataDto;
import org.com.dungeontalk.domain.aichat.dto.response.AiGameRoomResponse;
import org.com.dungeontalk.domain.aichat.entity.AiGameRoom;
import org.com.dungeontalk.domain.aichat.repository.AiGameRoomRepository;
import org.com.dungeontalk.domain.auth.service.ValkeyService;
import org.com.dungeontalk.global.exception.ErrorCode;
import org.com.dungeontalk.global.exception.customException.AiChatException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.com.dungeontalk.domain.aichat.common.AiChatConstants.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiGameStateService {

    private final AiGameRoomRepository aiGameRoomRepository;
    private final ValkeyService valkeyService;
    private final AiGameRoomService aiGameRoomService;
    private final ObjectMapper objectMapper;

    // 상수들을 AiChatConstants로 이동

    /**
     * 게임 세션 시작
     */
    @Transactional
    public AiGameRoomResponse startGameSession(String aiGameRoomId) {
        AiGameRoom room = aiGameRoomService.getGameRoomEntity(aiGameRoomId);

        if (room.getStatus() != AiGameStatus.CREATED) {
            throw new AiChatException(ErrorCode.AI_GAME_ROOM_INVALID_STATE);
        }

        // MongoDB에서 게임 상태 변경
        room.setStatus(AiGameStatus.ACTIVE);
        room.setCurrentPhase(AiGamePhase.TURN_INPUT);
        room.setLastActivity(LocalDateTime.now());

        AiGameRoom saved = aiGameRoomRepository.save(room);

        // Valkey에 게임 세션 정보 저장
        String sessionKey = AI_GAME_SESSION_PREFIX + aiGameRoomId;
        valkeyService.setWithExpiration(sessionKey, createSessionData(saved), DEFAULT_SESSION_TIMEOUT_SECONDS);

        log.info("AI 게임 세션 시작: roomId={}, participants={}", 
                 aiGameRoomId, saved.getParticipants());

        return AiGameRoomResponse.fromEntity(saved);
    }

    /**
     * 턴 진행 상태 변경
     */
    @Transactional
    public void changePhase(String aiGameRoomId, AiGamePhase newPhase) {
        AiGameRoom room = aiGameRoomService.getGameRoomEntity(aiGameRoomId);

        if (room.getStatus() != AiGameStatus.ACTIVE) {
            throw new AiChatException(ErrorCode.AI_GAME_ROOM_INVALID_STATE);
        }

        room.setCurrentPhase(newPhase);
        room.setLastActivity(LocalDateTime.now());

        aiGameRoomRepository.save(room);

        // Valkey 세션 정보 업데이트
        updateSessionPhase(aiGameRoomId, newPhase);

        log.info("AI 게임 페이즈 변경: roomId={}, newPhase={}", aiGameRoomId, newPhase);
    }

    /**
     * 다음 턴으로 진행
     */
    @Transactional
    public int nextTurn(String aiGameRoomId) {
        AiGameRoom room = aiGameRoomService.getGameRoomEntity(aiGameRoomId);

        if (room.getStatus() != AiGameStatus.ACTIVE) {
            throw new IllegalStateException("턴을 진행할 수 없는 게임 상태입니다: " + room.getStatus());
        }

        int newTurn = room.getCurrentTurn() + 1;
        room.setCurrentTurn(newTurn);
        room.setCurrentPhase(AiGamePhase.TURN_INPUT);
        room.setLastActivity(LocalDateTime.now());

        aiGameRoomRepository.save(room);

        // Valkey 세션 정보 업데이트
        updateSessionTurn(aiGameRoomId, newTurn);

        log.info("AI 게임 턴 진행: roomId={}, newTurn={}", aiGameRoomId, newTurn);
        return newTurn;
    }

    /**
     * AI 응답 중 상태로 변경 (메시지 블록)
     */
    public boolean lockForAiResponse(String aiGameRoomId) {
        String lockKey = AI_GAME_TURN_LOCK_PREFIX + aiGameRoomId;
        boolean locked = valkeyService.setIfNotExists(lockKey, "AI_PROCESSING", DEFAULT_TURN_LOCK_TIMEOUT_SECONDS);

        if (locked) {
            changePhase(aiGameRoomId, AiGamePhase.AI_RESPONSE);
            log.info("AI 응답 중 락 설정: roomId={}", aiGameRoomId);
        } else {
            log.warn("AI 응답 중 락 설정 실패 (이미 처리중): roomId={}", aiGameRoomId);
        }

        return locked;
    }

    /**
     * AI 응답 완료 후 락 해제
     */
    public void unlockAfterAiResponse(String aiGameRoomId) {
        String lockKey = AI_GAME_TURN_LOCK_PREFIX + aiGameRoomId;
        valkeyService.delete(lockKey);

        changePhase(aiGameRoomId, AiGamePhase.TURN_INPUT);
        log.info("AI 응답 완료 락 해제: roomId={}", aiGameRoomId);
    }

    /**
     * 게임 종료
     */
    @Transactional
    public void endGame(String aiGameRoomId) {
        AiGameRoom room = aiGameRoomService.getGameRoomEntity(aiGameRoomId);

        room.setStatus(AiGameStatus.COMPLETED);
        room.setCurrentPhase(AiGamePhase.GAME_END);
        room.setLastActivity(LocalDateTime.now());

        aiGameRoomRepository.save(room);

        // Valkey 세션 정보 삭제
        String sessionKey = AI_GAME_SESSION_PREFIX + aiGameRoomId;
        String lockKey = AI_GAME_TURN_LOCK_PREFIX + aiGameRoomId;
        valkeyService.delete(sessionKey);
        valkeyService.delete(lockKey);

        log.info("AI 게임 종료: roomId={}", aiGameRoomId);
    }

    /**
     * 게임 일시정지
     */
    @Transactional
    public void pauseGame(String aiGameRoomId, String reason) {
        AiGameRoom room = aiGameRoomService.getGameRoomEntity(aiGameRoomId);

        if (room.getStatus() != AiGameStatus.ACTIVE) {
            throw new IllegalStateException("일시정지할 수 없는 게임 상태입니다: " + room.getStatus());
        }

        room.setStatus(AiGameStatus.PAUSED);
        room.setLastActivity(LocalDateTime.now());

        aiGameRoomRepository.save(room);

        // 락 해제 (일시정지 중에는 AI 처리 중단)
        String lockKey = AI_GAME_TURN_LOCK_PREFIX + aiGameRoomId;
        valkeyService.delete(lockKey);

        log.info("AI 게임 일시정지: roomId={}, reason={}", aiGameRoomId, reason);
    }

    /**
     * 게임 재개
     */
    @Transactional
    public void resumeGame(String aiGameRoomId) {
        AiGameRoom room = aiGameRoomService.getGameRoomEntity(aiGameRoomId);

        if (room.getStatus() != AiGameStatus.PAUSED) {
            throw new AiChatException(ErrorCode.AI_GAME_ROOM_INVALID_STATE);
        }

        room.setStatus(AiGameStatus.ACTIVE);
        room.setCurrentPhase(AiGamePhase.TURN_INPUT);
        room.setLastActivity(LocalDateTime.now());

        aiGameRoomRepository.save(room);

        log.info("AI 게임 재개: roomId={}", aiGameRoomId);
    }

    /**
     * 세션 유효성 검증
     */
    public boolean isSessionValid(String aiGameRoomId) {
        String sessionKey = AI_GAME_SESSION_PREFIX + aiGameRoomId;
        return valkeyService.exists(sessionKey);
    }

    /**
     * AI 응답 처리 중인지 확인
     */
    public boolean isAiProcessing(String aiGameRoomId) {
        String lockKey = AI_GAME_TURN_LOCK_PREFIX + aiGameRoomId;
        return valkeyService.exists(lockKey);
    }

    /**
     * 세션 만료 시간 연장
     */
    public void extendSession(String aiGameRoomId) {
        String sessionKey = AI_GAME_SESSION_PREFIX + aiGameRoomId;
        if (valkeyService.exists(sessionKey)) {
            valkeyService.expire(sessionKey, DEFAULT_SESSION_TIMEOUT_SECONDS);
            log.debug("AI 게임 세션 연장: roomId={}", aiGameRoomId);
        }
    }

    /**
     * 비활성 게임 정리
     */
    @Transactional
    public void cleanupInactiveGames(int hoursAgo) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hoursAgo);
        List<AiGameRoom> inactiveRooms = aiGameRoomRepository.findByLastActivityBefore(cutoffTime);

        for (AiGameRoom room : inactiveRooms) {
            if (room.getStatus() == AiGameStatus.ACTIVE || room.getStatus() == AiGameStatus.PAUSED) {
                room.setStatus(AiGameStatus.COMPLETED);
                room.setCurrentPhase(AiGamePhase.GAME_END);
                        
                // Valkey 정리
                String sessionKey = AI_GAME_SESSION_PREFIX + room.getId();
                String lockKey = AI_GAME_TURN_LOCK_PREFIX + room.getId();
                valkeyService.delete(sessionKey);
                valkeyService.delete(lockKey);
            }
        }

        aiGameRoomRepository.saveAll(inactiveRooms);
        log.info("비활성 AI 게임 정리 완료: {} 개 게임방", inactiveRooms.size());
    }

    private String createSessionData(AiGameRoom room) {
        try {
            SessionDataDto sessionData = new SessionDataDto(
                    room.getId(),
                    room.getGameId(),
                    room.getStatus(),
                    room.getCurrentPhase(),
                    room.getCurrentTurn()
            );
            return objectMapper.writeValueAsString(sessionData);
        } catch (JsonProcessingException e) {
            log.error("세션 데이터 JSON 변환 실패: {}", e.getMessage());
            throw new AiChatException(ErrorCode.AI_RESPONSE_PROCESSING_ERROR, e);
        }
    }

    private void updateSessionPhase(String aiGameRoomId, AiGamePhase newPhase) {
        String sessionKey = AI_GAME_SESSION_PREFIX + aiGameRoomId;
        if (valkeyService.exists(sessionKey)) {
            String sessionData = valkeyService.get(sessionKey);
            // JSON 업데이트 로직 (간단하게 재생성)
            try {
                AiGameRoom room = aiGameRoomService.getGameRoomEntity(aiGameRoomId);
                valkeyService.setWithExpiration(sessionKey, createSessionData(room), DEFAULT_SESSION_TIMEOUT_SECONDS);
            } catch (AiChatException e) {
                log.warn("세션 업데이트 중 게임방을 찾을 수 없음: {}", aiGameRoomId);
            }
        }
    }

    private void updateSessionTurn(String aiGameRoomId, int newTurn) {
        String sessionKey = AI_GAME_SESSION_PREFIX + aiGameRoomId;
        if (valkeyService.exists(sessionKey)) {
            try {
                AiGameRoom room = aiGameRoomService.getGameRoomEntity(aiGameRoomId);
                valkeyService.setWithExpiration(sessionKey, createSessionData(room), DEFAULT_SESSION_TIMEOUT_SECONDS);
            } catch (AiChatException e) {
                log.warn("세션 업데이트 중 게임방을 찾을 수 없음: {}", aiGameRoomId);
            }
        }
    }

}