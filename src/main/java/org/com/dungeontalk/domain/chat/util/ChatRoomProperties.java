package org.com.dungeontalk.domain.chat.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "chat.room")
public class ChatRoomProperties {
    /** 방 생성 시 maxCapacity 미지정일 때 사용할 기본값. 0/음수 = 무제한 */

    @Value("${chat.room.default-max-capacity}")
    private Long defaultMaxCapacity;
}
