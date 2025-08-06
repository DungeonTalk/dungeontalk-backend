package org.com.dungeontalk.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 템플릿 : 상태 코드 - [에러가 난 도메인]+[세부 숫자]
    // 상태 코드는 RsData에서 추출 ?  혹은 여기서 명시?

    /* 예시 : 5xx */
    GLOBAL_ERROR("500-GL01", "서버 오류"),
    DATABASE_ERROR("500-DB01","데이터 베이스 오류");

    private final String errorCode;
    private final String message;

}
