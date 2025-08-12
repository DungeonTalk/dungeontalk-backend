package org.com.dungeontalk.domain.matching.exception;

import org.com.dungeontalk.global.exception.ErrorCode;

public class MatchingException extends RuntimeException {
    
    private final ErrorCode errorCode;

    public MatchingException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public MatchingException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}