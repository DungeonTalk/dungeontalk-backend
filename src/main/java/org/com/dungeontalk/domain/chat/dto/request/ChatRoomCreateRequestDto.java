package org.com.dungeontalk.domain.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;
import org.com.dungeontalk.domain.chat.common.ChatMode;
import org.com.dungeontalk.domain.chat.common.ChatRoomType;

@Getter
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomCreateRequestDto {
    @NotBlank
    @Size(max = 60)
    @Schema(example = "던전 1번방")
    private String roomName;

    @NotNull
    @Schema(example = "MULTI", allowableValues = {"SINGLE","MULTI"})
    private ChatMode mode;

    @Schema(description="참가자 ID 목록(선택)")
    @Size(max = 1000)
    private List<@NotBlank @Size(max=80) String> participantIds;

    @Schema(description="정원 (null = 기본, 0 또는 음수 = 무제한)", example = "8")
    private Integer maxCapacity;            // 선택 입력 (null이면 기본값 사용)
}
