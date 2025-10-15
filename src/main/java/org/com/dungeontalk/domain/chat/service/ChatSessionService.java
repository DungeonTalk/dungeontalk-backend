package org.com.dungeontalk.domain.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.auth.service.ValkeyService;
import org.com.dungeontalk.domain.chat.common.Status;
import org.com.dungeontalk.domain.chat.dto.ChatSessionDto;
import org.com.dungeontalk.domain.chat.entity.ChatRoom;
import org.com.dungeontalk.domain.chat.repository.ChatRoomRepository;
import org.com.dungeontalk.global.exception.ErrorCode;
import org.com.dungeontalk.global.exception.customException.ChatException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.com.dungeontalk.domain.chat.common.ChatConstants.*;

/**
 * 채팅 세션 관리 서비스
 * AI 게임 채팅의 AiGameStateService와 동일한 수준의 세션 관리로 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatSessionService {

    private final ValkeyService valkeyService;
    private final ChatRoomRepository chatRoomRepository;
    private final ObjectMapper objectMapper;

    /**
     * 세션 시작 (채팅방 입장)
     */
    @Transactional
    public ChatSessionDto startSession(String roomId, String memberId, String nickname, String websocketSessionId) {
        log.info("채팅 세션 시작 요청: roomId={}, memberId={}, nickname={}", roomId, memberId, nickname);

        // 채팅방 존재 확인
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND, "roomId=" + roomId));

        Instant now = Instant.now();

        // 세션 데이터 생성
        ChatSessionDto sessionDto = ChatSessionDto.builder()
                .roomId(roomId)
                .memberId(memberId)
                .nickname(nickname)
                .status(Status.ONLINE)
                .joinedAt(now)
                .lastActivity(now)
                .websocketSessionId(websocketSessionId)
                .build();

        // Valkey에 세션 저장
        String sessionKey = buildSessionKey(roomId, memberId);
        String sessionData = serializeSessionData(sessionDto);

        log.info("Valkey 세션 저장 시도: sessionKey={}, timeout={}초", sessionKey, DEFAULT_SESSION_TIMEOUT_SECONDS);
        valkeyService.setWithExpiration(sessionKey, sessionData, DEFAULT_SESSION_TIMEOUT_SECONDS);

        // 저장 확인
        boolean sessionExists = valkeyService.exists(sessionKey);

        log.info("Valkey 세션 저장 결과: sessionKey={}, exists={}", sessionKey, sessionExists);
        log.info("채팅 세션 시작 완료: roomId={}, memberId={}", roomId, memberId);

        return sessionDto;
    }

    /**
     * 세션 연장 (활동 시간 갱신)
     */
    public void extendSession(String roomId, String memberId) {
        String sessionKey = buildSessionKey(roomId, memberId);

        if (!valkeyService.exists(sessionKey)) {
            log.warn("세션 연장 실패 - 세션이 존재하지 않음: roomId={}, memberId={}", roomId, memberId);
            return;
        }

        try {
            // 기존 세션 데이터 조회
            String sessionData = valkeyService.get(sessionKey);
            ChatSessionDto sessionDto = objectMapper.readValue(sessionData, ChatSessionDto.class);

            // 마지막 활동 시간 갱신
            ChatSessionDto updatedSession = sessionDto.toBuilder()
                    .lastActivity(Instant.now())
                    .build();

            // Valkey에 갱신된 세션 저장 및 TTL 연장
            valkeyService.setWithExpiration(sessionKey, serializeSessionData(updatedSession), DEFAULT_SESSION_TIMEOUT_SECONDS);

            log.debug("채팅 세션 연장: roomId={}, memberId={}", roomId, memberId);
        } catch (JsonProcessingException e) {
            log.error("세션 데이터 역직렬화 실패: roomId={}, memberId={}", roomId, memberId, e);
        }
    }

    /**
     * Heartbeat 처리 (세션 유지)
     */
    public boolean heartbeat(String roomId, String memberId) {
        String sessionKey = buildSessionKey(roomId, memberId);

        if (!valkeyService.exists(sessionKey)) {
            log.warn("Heartbeat 실패 - 세션이 존재하지 않음: roomId={}, memberId={}", roomId, memberId);
            return false;
        }

        // TTL만 연장
        valkeyService.expire(sessionKey, DEFAULT_SESSION_TIMEOUT_SECONDS);
        log.debug("Heartbeat 처리: roomId={}, memberId={}", roomId, memberId);

        return true;
    }

    /**
     * 세션 종료 (채팅방 퇴장)
     */
    @Transactional
    public void endSession(String roomId, String memberId) {
        String sessionKey = buildSessionKey(roomId, memberId);

        if (!valkeyService.exists(sessionKey)) {
            log.debug("세션 종료 - 세션이 이미 없음: roomId={}, memberId={}", roomId, memberId);
            return;
        }

        // Valkey 세션 삭제
        valkeyService.delete(sessionKey);

        log.info("채팅 세션 종료: roomId={}, memberId={}", roomId, memberId);
    }

    /**
     * 세션 유효성 검증
     */
    public boolean isSessionValid(String roomId, String memberId) {
        String sessionKey = buildSessionKey(roomId, memberId);
        boolean exists = valkeyService.exists(sessionKey);

        log.debug("세션 유효성 검증: roomId={}, memberId={}, exists={}", roomId, memberId, exists);

        return exists;
    }

    /**
     * 세션 정보 조회
     */
    public ChatSessionDto getSession(String roomId, String memberId) {
        String sessionKey = buildSessionKey(roomId, memberId);

        if (!valkeyService.exists(sessionKey)) {
            log.debug("세션 조회 실패 - 세션이 존재하지 않음: roomId={}, memberId={}", roomId, memberId);
            return null;
        }

        try {
            String sessionData = valkeyService.get(sessionKey);
            return objectMapper.readValue(sessionData, ChatSessionDto.class);
        } catch (JsonProcessingException e) {
            log.error("세션 데이터 역직렬화 실패: roomId={}, memberId={}", roomId, memberId, e);
            return null;
        }
    }

    /**
     * 채팅방의 모든 활성 세션 조회
     */
    public List<ChatSessionDto> getActiveSessionsInRoom(String roomId) {
        String pattern = buildSessionKeyPattern(roomId);
        Set<String> keys = valkeyService.keys(pattern);

        List<ChatSessionDto> sessions = new ArrayList<>();

        if (keys == null || keys.isEmpty()) {
            return sessions;
        }

        for (String key : keys) {
            try {
                String sessionData = valkeyService.get(key);
                if (sessionData != null) {
                    ChatSessionDto session = objectMapper.readValue(sessionData, ChatSessionDto.class);
                    sessions.add(session);
                }
            } catch (JsonProcessingException e) {
                log.error("세션 데이터 역직렬화 실패: key={}", key, e);
            }
        }

        return sessions;
    }

    /**
     * 비활성 세션 정리
     * TTL 기반이므로 자동 만료되지만, 필요시 명시적 정리 가능
     */
    @Transactional
    public void cleanupInactiveSessions(String roomId) {
        log.info("비활성 세션 정리 시작: roomId={}", roomId);

        List<ChatSessionDto> sessions = getActiveSessionsInRoom(roomId);
        Instant now = Instant.now();
        int cleanedCount = 0;

        for (ChatSessionDto session : sessions) {
            // INACTIVE_SESSION_CLEANUP_HOURS 시간 이상 활동이 없는 세션 정리
            if (session.getLastActivity() != null) {
                long hoursSinceActivity = java.time.Duration.between(session.getLastActivity(), now).toHours();

                if (hoursSinceActivity >= INACTIVE_SESSION_CLEANUP_HOURS) {
                    endSession(session.getRoomId(), session.getMemberId());
                    cleanedCount++;
                    log.info("비활성 세션 정리: roomId={}, memberId={}, hoursSinceActivity={}",
                            session.getRoomId(), session.getMemberId(), hoursSinceActivity);
                }
            }
        }

        log.info("비활성 세션 정리 완료: roomId={}, cleanedCount={}", roomId, cleanedCount);
    }

    /**
     * 채팅방의 모든 세션 종료 (방 삭제 시)
     */
    @Transactional
    public void endAllSessionsInRoom(String roomId) {
        log.info("채팅방 전체 세션 종료 시작: roomId={}", roomId);

        List<ChatSessionDto> sessions = getActiveSessionsInRoom(roomId);

        for (ChatSessionDto session : sessions) {
            endSession(session.getRoomId(), session.getMemberId());
        }

        log.info("채팅방 전체 세션 종료 완료: roomId={}, sessionCount={}", roomId, sessions.size());
    }

    /**
     * 세션 키 생성
     */
    private String buildSessionKey(String roomId, String memberId) {
        return CHAT_SESSION_PREFIX + roomId + ":" + memberId;
    }

    /**
     * 세션 키 패턴 생성 (특정 방의 모든 세션 조회용)
     */
    private String buildSessionKeyPattern(String roomId) {
        return CHAT_SESSION_PREFIX + roomId + ":*";
    }

    /**
     * 세션 데이터 직렬화
     */
    private String serializeSessionData(ChatSessionDto sessionDto) {
        try {
            return objectMapper.writeValueAsString(sessionDto);
        } catch (JsonProcessingException e) {
            log.error("세션 데이터 JSON 변환 실패: {}", e.getMessage());
            throw new ChatException(ErrorCode.CHAT_INVALID_PAYLOAD, e);
        }
    }
}
