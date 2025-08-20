package org.com.dungeontalk.domain.gamecharacter.dto.projection;

import java.time.Instant;

/**
 * 게임 캐릭터 상세 정보 Projection
 * N+1 쿼리 문제 해결을 위한 인터페이스 기반 Projection
 * 
 * MongoDB와 JPA를 함께 사용하는 환경에서 
 * JOIN 쿼리를 통해 한 번에 필요한 데이터를 가져오기 위함
 * 
 * 보안상 memberId는 제외됨
 */
public interface GameCharacterDetailProjection {
    // GameCharacter fields (memberId 제외)
    String getId();
    String getRaceId();
    Integer getPlayerLevel();
    Long getTotalExp();
    Integer getUnspentPoints();
    Integer getStrength();
    Integer getWillpower();
    Integer getIntelligence();
    Integer getWisdom();
    Integer getDexterity();
    Integer getLuck();
    Instant getCreatedAt();
    Instant getUpdatedAt();
    
    // Member fields (JOIN)
    String getNickname();
    
    // RaceStats fields (JOIN)
    String getRaceName();
    
    // Calculated fields (will be added separately)
    // These fields are not directly from database but calculated
    default Double getHealthPoints() { return null; }
    default Double getManaPoints() { return null; }
    default Double getPhysicalAttack() { return null; }
    default Double getMagicAttack() { return null; }
    default Double getEvasionRate() { return null; }
    default Double getAccuracy() { return null; }
    default Double getDiceOdds() { return null; }
}