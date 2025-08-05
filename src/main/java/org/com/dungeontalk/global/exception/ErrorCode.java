package org.com.dungeontalk.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    /* Global Exceoption 0xx */
    DATABASE_ERROR("GLOBAL_ERROR_001","데이터 베이스 오류",HttpStatus.INTERNAL_SERVER_ERROR);

    /**
     * 아직 Domain이 분할되지 않았으니 아직 영역을 지정하지는 않았습니다.
     * Domain이 명확이 분할 & 지정이 되면 다음과 같은 예시로 분할하여 에러 코드를 관리 하겠습니다.
     *
     *  - Member Exception 6XX
     *  - Book Exception 7XX
     *  - Review Exception 8XX
     *
     */

    private final String errorCode;
    private final String message;
    private final HttpStatus httpStatus;
}
