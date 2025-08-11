package org.com.dungeontalk.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 템플릿 : 상태 코드 - [에러가 난 도메인]+[세부 숫자]
    // 상태 코드는 RsData에서 추출 ?  혹은 여기서 명시?

    /* 클라이언트 에러 */
    INVALID_JWT_TOKEN("401-AU01", "유효하지 않은 JWT 토큰입니다."),
    EXPIRED_JWT_TOKEN("401-AU02", "만료된 JWT 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND("401-AU03", "저장소에 존재하지 않는 리프레시 토큰입니다."),

    /* 서버 에러 */
    GLOBAL_ERROR("500-GL01", "서버 글로벌 오류"),
    DATABASE_ERROR("500-DB01","데이터 베이스 오류");



    private final String errorCode;
    private final String message;

}
