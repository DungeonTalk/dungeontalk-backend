package org.com.dungeontalk.global.exception;

import org.com.dungeontalk.global.exception.customException.ChatException;
import org.com.dungeontalk.global.exception.customException.MemberException;
import org.com.dungeontalk.global.rsData.Empty;
import org.com.dungeontalk.global.rsData.RsData;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Member Domain Exception Handler
    @ExceptionHandler(MemberException.class)
    public RsData<Empty> handleCustomException(MemberException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return RsData.of(errorCode.getErrorCode(), errorCode.getMessage(), new Empty());
    }

    @ExceptionHandler(ChatException.class)
    public RsData<Empty> handleChatException(ChatException ex) {
        ErrorCode errorCode = ex.getErrorCode();

        // 필요 시 ex.getMessage()로 커스텀 메시지 노출 가능
        return RsData.of(errorCode.getErrorCode(), errorCode.getMessage(), new Empty());
    }

}
