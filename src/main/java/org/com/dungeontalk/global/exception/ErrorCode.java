package org.com.dungeontalk.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    /* Global Exceoption 0xx */
    DATABASE_ERROR("GLOBAL_ERROR_001","데이터 베이스 오류",HttpStatus.INTERNAL_SERVER_ERROR),

    /* Member Exception 6XX */
    ALREADY_EXIST_NICKNAME("MEMBER_ERROR_600", "이미 존재하는 닉네임입니다.", HttpStatus.CONFLICT),
    ALREADY_EXIST_PHONE_NUMBER("MEMBER_ERROR_601", "이미 등록된 전화번호 입니다.", HttpStatus.CONFLICT),
    USER_NOT_FOUND("MEMBER_ERROR_602", "해당 닉네임으로 조회되는 유저가 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_PASSWORD("MEMBER_ERROR_603", "비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_NOT_MATCHED("MEMBER_ERROR_604", "제출하신 리프레시 토큰이 세션에 저장된 리프레시 토큰과 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),
    NO_AUTH("MEMBER_ERROR_605","본 유저에게 해당 서비스를 요청할 권한이 없습니다.",HttpStatus.UNAUTHORIZED);


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
