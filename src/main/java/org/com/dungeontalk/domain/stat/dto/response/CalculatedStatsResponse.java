package org.com.dungeontalk.domain.stat.dto.response;

import java.time.Instant;
import java.util.Map;

public record CalculatedStatsResponse(
        String characterId,
        double hp,
        double mp,
        double physicalAttack,
        double magicAttack,
        double evasionRate,
        double accuracy,
        double diceOdds,
        Instant calculatedAt
) {
    public static CalculatedStatsResponse fromMap(String characterId, Map<String, Double> m) {
        return new CalculatedStatsResponse(
                characterId,
                m.getOrDefault("hp", 0d),
                m.getOrDefault("mp", 0d),
                m.getOrDefault("physicalAttack", 0d),
                m.getOrDefault("magicAttack", 0d),
                m.getOrDefault("evasionRate", 0d),
                m.getOrDefault("accuracy", 0d),
                m.getOrDefault("diceOdds", 0d),
                Instant.now()
        );
    }
}

