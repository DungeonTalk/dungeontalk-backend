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

    // 로그 가독성 개선: [코드] 기본메시지 - 커스텀메시지
    @Override
    public String getMessage() {
        String base = (super.getMessage() != null ? super.getMessage() : "");
        String prefix = "[" + errorCode.getErrorCode() + "] " + errorCode.getMessage();
        return base.isEmpty() ? prefix : (prefix + " - " + base);
    }

}
