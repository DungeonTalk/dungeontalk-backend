package org.com.dungeontalk.global.exception.customException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public class AiChatException extends RuntimeException {
    private final ErrorCode errorCode;
    
    public AiChatException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
    
    public AiChatException(ErrorCode errorCode, Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
    }
}