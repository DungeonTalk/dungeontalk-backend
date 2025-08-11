package org.com.dungeontalk.domain.matching.factory;

import lombok.Builder;
import lombok.Value;
import org.com.dungeontalk.domain.matching.common.WorldType;

import java.util.List;

/**
 * 매칭 방 생성을 위한 컨텍스트 정보
 */
@Value
@Builder
public class RoomCreationContext {
    
    /**
     * 게임 세션 ID
     */
    String gameSessionId;
    
    /**
     * 참가자 목록
     */
    List<String> participants;
    
    /**
     * 월드 타입
     */
    WorldType worldType;
}