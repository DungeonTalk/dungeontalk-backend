package org.com.dungeontalk.global.exception.customException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public class ChatException extends RuntimeException {

    private final ErrorCode errorCode;

    public ChatException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    public ChatException(ErrorCode errorCode, Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
    }

}
