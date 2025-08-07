package org.com.dungeontalk.domain.aichat.common;

/**
 * AI 게임의 진행 단계를 나타내는 Enum
 * 
 * Valkey에서 게임 상태 관리 및 WebSocket 메시지에서 사용
 */
public enum AiGamePhase {
    
    /**
     * 플레이어들이 게임방에 입장하기를 기다리는 단계
     * - 3명 미만일 때의 상태
     */
    WAITING,
    
    /**
     * 플레이어들이 턴 입력을 하는 단계
     * - 3명의 플레이어가 각자 메시지를 입력하는 시간
     * - 모든 플레이어가 입력할 때까지 대기
     */
    TURN_INPUT,
    
    /**
     * AI가 응답을 생성하고 있는 단계  
     * - Python AI 서비스에서 응답 생성 중
     * - 모든 플레이어의 메시지 전송이 차단됨
     */
    AI_RESPONSE,
    
    /**
     * 게임이 종료된 상태
     * - 플레이어가 게임 종료를 선택했거나
     * - 에러로 인해 게임이 중단된 경우
     */
    GAME_END;

    // @Enumerated 반영 안 되는 이슈 처리 (기존 컨벤션 따름)
    @Override
    public String toString() {
        return name();  // name() == "WAITING", "TURN_INPUT" 등
    }
}