package org.com.dungeontalk.global.rsData;

// 간단한 static 메서드로 HTTP 상태 코드 기반 성공/실패 판정 제공.
public class ResponseStatus {

    private ResponseStatus() {}

    /**
     * 성공 판별 메서드 : statusCode가 200 이상 399 이하라면 성공으로 간주
     *
     * @param statusCode 상태 코드
     * @return T,F
     */
    public static boolean isSuccess(int statusCode) {
        return statusCode >= 200 && statusCode < 400;
    }

    /**
     * 성공 판별 메서드 : !isSuccess(statusCode)일 시 T 반환
     *
     * @param statusCode 상태 코드
     * @return T,F
     */
    public static boolean isFail(int statusCode) {
        return !isSuccess(statusCode);
    }
}
