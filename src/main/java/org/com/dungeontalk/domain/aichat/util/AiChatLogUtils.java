package org.com.dungeontalk.domain.aichat.util;

import lombok.extern.slf4j.Slf4j;

/**
 * AI 채팅 관련 로깅을 위한 공통 유틸리티
 * 일관된 로그 포맷으로 동료 개발자들이 로그를 쉽게 추적할 수 있도록 도와줍니다.
 */
@Slf4j
public class AiChatLogUtils {

    /**
     * 게임 액션 로깅 (성공)
     */
    public static void logGameAction(String action, String roomId, Object... params) {
        if (params.length > 0) {
            log.info("🎮 {} 완료: roomId={}, params={}", action, roomId, params);
        } else {
            log.info("🎮 {} 완료: roomId={}", action, roomId);
        }
    }

    /**
     * 게임 액션 로깅 (시작)
     */
    public static void logGameActionStart(String action, String roomId, Object... params) {
        if (params.length > 0) {
            log.debug("🎯 {} 시작: roomId={}, params={}", action, roomId, params);
        } else {
            log.debug("🎯 {} 시작: roomId={}", action, roomId);
        }
    }

    /**
     * AI 응답 관련 로깅
     */
    public static void logAiResponse(String roomId, int turnNumber, Long responseTime) {
        log.info("🤖 AI 응답 생성: roomId={}, turn={}, responseTime={}ms", 
                roomId, turnNumber, responseTime);
    }

    /**
     * 턴 진행 로깅
     */
    public static void logTurnProgress(String roomId, int currentTurn, int nextTurn) {
        log.info("🔄 턴 진행: roomId={}, turn {} → {}", roomId, currentTurn, nextTurn);
    }

    /**
     * 메시지 전송 로깅
     */
    public static void logMessageSent(String roomId, String senderId, String messageType) {
        log.info("💬 메시지 전송: roomId={}, sender={}, type={}", 
                roomId, senderId, messageType);
    }

    /**
     * WebSocket 브로드캐스트 로깅
     */
    public static void logWebSocketBroadcast(String roomId, String messageType) {
        log.debug("📡 WebSocket 브로드캐스트: roomId={}, type={}", roomId, messageType);
    }

    /**
     * 게임방 상태 변경 로깅
     */
    public static void logGameStateChange(String roomId, String fromState, String toState) {
        log.info("🔀 게임 상태 변경: roomId={}, {} → {}", roomId, fromState, toState);
    }

    /**
     * 성능 측정 로깅
     */
    public static void logPerformance(String operation, String roomId, long durationMs) {
        if (durationMs > 1000) {
            log.warn("⚠️  성능 주의 - {}: roomId={}, duration={}ms", operation, roomId, durationMs);
        } else {
            log.debug("⏱️  성능 측정 - {}: roomId={}, duration={}ms", operation, roomId, durationMs);
        }
    }

    /**
     * 에러 로깅 (공통 포맷)
     */
    public static void logError(String operation, String roomId, Exception e) {
        log.error("❌ {} 실패: roomId={}, error={}", operation, roomId, e.getMessage(), e);
    }

    /**
     * 경고 로깅
     */
    public static void logWarning(String operation, String roomId, String message) {
        log.warn("⚠️  {} 경고: roomId={}, message={}", operation, roomId, message);
    }
}