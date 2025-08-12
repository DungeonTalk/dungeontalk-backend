package org.com.dungeontalk.domain.chat.event;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;
import org.com.dungeontalk.domain.chat.common.MessageType;

@Value
@Builder
public class ChatPresenceEvent {

    /**
     * 상태 기록 레이어(=ChatRoomMemberService)는 그대로 두고,
     * “실제 상태가 바뀐 경우에만” 도메인 이벤트를 발행
     */

    String roomId;
    String memberId;
    MessageType type;   // JOIN or LEAVE
    Instant occurredAt;

}
