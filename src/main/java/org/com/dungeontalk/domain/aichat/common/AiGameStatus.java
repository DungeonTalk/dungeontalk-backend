package org.com.dungeontalk.domain.aichat.common;

/**
 * AI 게임방의 전체 상태를 나타내는 Enum
 * 
 * 게임 라이프사이클 관리 및 방 입장 가능 여부 판단에 사용
 */
public enum AiGameStatus {
    
    /**
     * 게임방이 생성되었으나 아직 시작되지 않은 상태
     * - 플레이어들이 입장 가능한 상태
     * - 3명 미만일 때의 대기 상태
     */
    CREATED,
    
    /**
     * 게임이 진행 중인 상태
     * - 3명의 플레이어가 모두 입장하여 게임 시작
     * - 새로운 플레이어 입장 불가
     * - 턴제 게임이 활발히 진행 중
     */
    ACTIVE,
    
    /**
     * 게임이 일시정지된 상태
     * - 플레이어 중 일부가 연결 해제
     * - 시스템 점검 등으로 인한 일시정지
     * - 재시작 가능한 상태
     */
    PAUSED,
    
    /**
     * 게임이 정상적으로 완료된 상태
     * - 플레이어들이 게임 종료를 선택
     * - 스토리가 완결된 경우
     * - 재입장 불가, 히스토리만 조회 가능
     */
    COMPLETED,
    
    /**
     * 게임이 비정상적으로 종료된 상태
     * - 에러로 인한 강제 종료
     * - 플레이어 전원 이탈
     * - 시스템 장애 등
     */
    ERROR;

    // @Enumerated 반영 안 되는 이슈 처리 (기존 컨벤션 따름)
    @Override
    public String toString() {
        return name();  // name() == "CREATED", "ACTIVE", "COMPLETED" 등
    }
}