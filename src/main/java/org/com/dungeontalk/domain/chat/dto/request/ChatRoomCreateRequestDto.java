package org.com.dungeontalk.domain.chat.dto.request;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.com.dungeontalk.domain.chat.common.ChatMode;
import org.com.dungeontalk.domain.chat.common.ChatRoomType;

@Getter
@Setter
public class ChatRoomCreateRequestDto {
    private String roomName;
    private ChatMode mode;
    private List<String> participantIds;
    private Long maxCapacity;               // 선택 입력 (null이면 기본값 사용)
}
