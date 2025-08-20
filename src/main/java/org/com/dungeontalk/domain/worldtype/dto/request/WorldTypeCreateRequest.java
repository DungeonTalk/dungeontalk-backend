package org.com.dungeontalk.domain.worldtype.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 세계관 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WorldTypeCreateRequest {

    @NotBlank(message = "코드는 필수입니다")
    @Size(max = 50, message = "코드는 최대 50자까지 가능합니다")
    private String code;

    @NotBlank(message = "표시 이름은 필수입니다")
    @Size(max = 100, message = "표시 이름은 최대 100자까지 가능합니다")
    private String displayName;

    @NotBlank(message = "설명은 필수입니다")
    @Size(max = 500, message = "설명은 최대 500자까지 가능합니다")
    private String description;

    @NotBlank(message = "게임 설정은 필수입니다")
    @Size(max = 1000, message = "게임 설정은 최대 1000자까지 가능합니다")
    private String gameSettings;

    @NotNull(message = "정렬 순서는 필수입니다")
    private Integer sortOrder;
}