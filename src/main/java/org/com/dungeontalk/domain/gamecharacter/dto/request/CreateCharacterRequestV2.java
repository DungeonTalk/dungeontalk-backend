package org.com.dungeontalk.domain.gamecharacter.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 캐릭터 생성 요청 DTO V2
 * memberId를 제외하고 종족만 받음 (인증된 사용자 기반)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "캐릭터 생성 요청 V2")
public class CreateCharacterRequestV2 {
    
    @NotNull(message = "종족은 필수입니다")
    @Schema(description = "캐릭터 종족", example = "HUMAN", required = true, 
            allowableValues = {"HUMAN", "ELF", "DWARF", "ORC"})
    private String race;
    
    /**
     * V1 CreateCharacterRequest로 변환
     * @param memberId 인증된 사용자의 ID
     * @return CreateCharacterRequest
     */
    public CreateCharacterRequest toCreateCharacterRequest(String memberId) {
        return new CreateCharacterRequest(memberId, this.race);
    }
}