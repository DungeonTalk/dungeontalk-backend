package org.com.dungeontalk.global.exception;

import org.com.dungeontalk.global.exception.customException.MemberException;
import org.com.dungeontalk.global.rsData.Empty;
import org.com.dungeontalk.global.rsData.RsData;
import org.com.dungeontalk.global.rsData.RsDataFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Member Domain Exception Handler
    @ExceptionHandler(MemberException.class)
    public ResponseEntity<RsData<Empty>> handleCustomException(MemberException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(RsDataFactory.of(errorCode.getErrorCode(), errorCode.getMessage(), new Empty()));
    }

}
