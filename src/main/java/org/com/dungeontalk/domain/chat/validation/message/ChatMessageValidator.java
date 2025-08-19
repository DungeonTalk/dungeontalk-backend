package org.com.dungeontalk.domain.chat.validation.message;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.com.dungeontalk.domain.chat.common.MessageType;
import org.com.dungeontalk.domain.chat.dto.request.ChatMessageSendRequestDto;

public class ChatMessageValidator implements ConstraintValidator<ValidChatMessage, ChatMessageSendRequestDto> {

    @Override
    public boolean isValid(ChatMessageSendRequestDto v, ConstraintValidatorContext c) {
        if (v == null || v.getType() == null) return false;

        if (v.getType() == MessageType.TALK) {
            return v.getContent() != null && !v.getContent().isBlank()
                && v.getRoomId() != null && !v.getRoomId().isBlank();
        }

        // JOIN / LEAVE / PRESENCE: roomId만 확인
        return v.getRoomId() != null && !v.getRoomId().isBlank();
    }
}
