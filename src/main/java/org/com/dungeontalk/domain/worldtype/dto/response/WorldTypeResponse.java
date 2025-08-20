package org.com.dungeontalk.domain.worldtype.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.com.dungeontalk.domain.worldtype.entity.WorldType;

import java.time.Instant;

/**
 * 세계관 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorldTypeResponse {

    private Long id;
    private String code;
    private String displayName;
    private String description;
    private String gameSettings;
    private Boolean isActive;
    private Integer sortOrder;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Entity에서 DTO로 변환
     */
    public static WorldTypeResponse from(WorldType worldType) {
        return WorldTypeResponse.builder()
                .id(worldType.getId())
                .code(worldType.getCode())
                .displayName(worldType.getDisplayName())
                .description(worldType.getDescription())
                .gameSettings(worldType.getGameSettings())
                .isActive(worldType.getIsActive())
                .sortOrder(worldType.getSortOrder())
                .createdAt(worldType.getCreatedAt())
                .updatedAt(worldType.getUpdatedAt())
                .build();
    }
}