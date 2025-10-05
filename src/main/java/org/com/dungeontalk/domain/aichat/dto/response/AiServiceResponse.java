package org.com.dungeontalk.domain.aichat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class AiServiceResponse {
    private String content;
    private Long responseTime;
    private List<String> sources;
    private String worldType;
    private List<String> docTypesUsed;
    private Map<String, Object> gameTimeInfo;  // 게임 시간 정보
    
    /**
     * 게임이 종료되었는지 확인
     */
    public boolean isGameEnded() {
        if (gameTimeInfo == null) return false;
        Boolean gameEnded = (Boolean) gameTimeInfo.get("game_ended");
        return gameEnded != null && gameEnded;
    }
    
    /**
     * 게임 단계 반환 (도입, 전개, 중반, 클라이맥스, 종료)
     */
    public String getGamePhase() {
        if (gameTimeInfo == null) return "알 수 없음";
        return (String) gameTimeInfo.getOrDefault("game_phase", "알 수 없음");
    }
    
    /**
     * 남은 시간 반환 (분)
     */
    public Integer getRemainingTime() {
        if (gameTimeInfo == null) return null;
        return (Integer) gameTimeInfo.get("remaining_time");
    }
    
    /**
     * 게임 결과 반환 (SUCCESS, FAILURE, TIMEOUT, UNKNOWN)
     */
    public String getGameResult() {
        if (gameTimeInfo == null) return "UNKNOWN";
        return (String) gameTimeInfo.getOrDefault("game_result", "UNKNOWN");
    }
}