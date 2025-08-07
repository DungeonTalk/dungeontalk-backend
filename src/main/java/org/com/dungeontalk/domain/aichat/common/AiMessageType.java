package org.com.dungeontalk.domain.aichat.common;

/**
 * AI 채팅 메시지의 타입을 구분하는 Enum
 * 
 * MongoDB 저장 시 메시지 구분 및 프론트엔드 UI에서 사용
 */
public enum AiMessageType {
    
    /**
     * 사용자가 보낸 메시지
     * - 플레이어가 턴에서 입력한 행동/대화
     * - senderNickname으로 구분
     */
    USER,
    
    /**
     * AI(GM)가 생성한 응답 메시지
     * - Python AI 서비스에서 생성된 응답
     * - senderId는 "AI_GM"으로 고정
     */
    AI,
    
    /**
     * 시스템이 생성한 알림 메시지
     * - 플레이어 입장/퇴장 알림
     * - 게임 상태 변경 알림
     * - 에러 메시지 등
     */
    SYSTEM,
    
    /**
     * 새로운 턴 시작을 알리는 메시지
     * - 턴 번호와 함께 전송
     * - 플레이어들에게 입력 시작 신호
     */
    TURN_START,
    
    /**
     * 턴 종료를 알리는 메시지
     * - AI 응답 완료 후 전송
     * - 다음 턴 준비 신호
     */
    TURN_END;

    // @Enumerated 반영 안 되는 이슈 처리 (기존 컨벤션 따름)
    @Override
    public String toString() {
        return name();  // name() == "USER", "AI", "SYSTEM" 등
    }
}