package org.com.dungeontalk.domain.chat.dto;

import lombok.Builder;
import lombok.Getter;
import org.com.dungeontalk.domain.chat.common.Status;

@Getter
@Builder
public class MemberPresenceDto {
    private String memberId;
    private String nickname;
    private Status status;          // ONLINE / OFFLINE
}
