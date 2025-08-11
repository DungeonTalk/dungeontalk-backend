package org.com.dungeontalk.domain.aichat.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AI 채팅 관련 에러 처리 및 로깅을 위한 공통 유틸리티
 * 동료 개발자들이 매번 try-catch와 로깅을 반복해서 작성하지 않도록 도와줍니다.
 */
@Slf4j
@Component
public class AiChatErrorHandler {

    /**
     * 게임 액션 실행 시 공통 에러 처리 및 로깅
     * 
     * @param action 실행할 액션 (람다로 전달)
     * @param context 에러 발생 시 표시할 컨텍스트 (예: "메시지 전송", "게임방 생성")
     * @param roomId 게임방 ID (로깅용)
     * @param params 추가 로깅 파라미터들
     */
    public <T> T executeWithLogging(
            GameAction<T> action, 
            String context, 
            String roomId, 
            Object... params) {
        
        try {
            log.debug("{} 시작: roomId={}, params={}", context, roomId, params);
            T result = action.execute();
            log.info("{} 성공: roomId={}", context, roomId);
            return result;
            
        } catch (Exception e) {
            log.error("{} 실패: roomId={}, error={}", context, roomId, e.getMessage(), e);
            throw new RuntimeException(context + " 실행 중 오류 발생", e);
        }
    }

    /**
     * 게임 액션 실행 (반환값 없는 경우)
     */
    public void executeWithLogging(
            VoidGameAction action, 
            String context, 
            String roomId, 
            Object... params) {
        
        executeWithLogging(() -> {
            action.execute();
            return null;
        }, context, roomId, params);
    }

    /**
     * WebSocket 메시지 전송 에러 처리
     */
    public void handleWebSocketError(String roomId, Exception e) {
        log.error("WebSocket 메시지 전송 실패: roomId={}, error={}", roomId, e.getMessage());
        // 추후 WebSocket 연결 복구 로직 등을 여기에 추가 가능
    }

    /**
     * AI 응답 생성 에러 처리
     */
    public void handleAiResponseError(String roomId, Exception e) {
        log.error("AI 응답 생성 실패: roomId={}, error={}", roomId, e.getMessage());
        // 추후 AI 서비스 fallback 로직 등을 여기에 추가 가능
    }

    /**
     * 게임 상태 변경 에러 처리
     */
    public void handleStateChangeError(String roomId, String fromState, String toState, Exception e) {
        log.error("게임 상태 변경 실패: roomId={}, {} -> {}, error={}", 
                 roomId, fromState, toState, e.getMessage());
        // 추후 상태 롤백 로직 등을 여기에 추가 가능
    }

    @FunctionalInterface
    public interface GameAction<T> {
        T execute() throws Exception;
    }

    @FunctionalInterface
    public interface VoidGameAction {
        void execute() throws Exception;
    }
}