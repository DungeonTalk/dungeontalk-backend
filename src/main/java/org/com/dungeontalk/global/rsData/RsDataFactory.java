package org.com.dungeontalk.global.rsData;

import java.util.Objects;

// RsData 생성 전용 클래스 (정적 팩토리 메서드 집합)
public final class RsDataFactory {

    private RsDataFactory() {}

    // ======================= 기본 상수 =========================

    // 기본 성공 응답 (데이터 없음)
    public static final RsData<Empty> OK = of("200-1", "성공", new Empty());
    // 기본 실패 응답 (데이터 없음)
    public static final RsData<Empty> FAIL = of("500-1", "실패", new Empty());

    // ======================= RsData 생성 메서드 =========================

    /**
     * 메시지 만으로 성공 응답을 생성하는 메서드
     * @param msg 메시지
     * @return 성공 응답
     */
    public static RsData<Empty> of(String msg) {
        return of("200-1", msg, new Empty());
    }

    /**
     * 데이터 만으로 성공 응답을 생성하는 메서드
     * @param data 데이터
     * @return 성공 응답
     * @param <T> 페이로드의 타입
     */
    public static <T> RsData<T> of(T data) {
        return of("200-1", "성공", data);
    }

    /**
     * 데이터와 메시지로 성공 응답을 생성하는 메서드
     * @param msg 메시지
     * @param data 데이터
     * @return 성공 응답
     * @param <T> 페이로드의 타입
     */
    public static <T> RsData<T> of(String msg, T data) {
        return of("200-1", msg, data);
    }

    // resultCode + msg: data는 Empty
    /**
     * 결과 코드와 메시지로 성공 응답을 생성하는 메서드
     * @param resultCode 결과 코드
     * @param msg 메시지
     * @return 성공 응답
     * @param <T> 제네릭 타입
     */
    @SuppressWarnings("unchecked")
    public static <T> RsData<T> of(String resultCode, String msg) {
        return of(resultCode, msg, (T) new Empty());
    }

    /**
     * 결과 코드, 메시지, 데이터로 성공 응답을 생성하는 메서드
     * @param resultCode 결과 코드
     * @param msg 메시지
     * @param data 데이터
     * @return 성공 응답
     * @param <T> 페이로드의 타입
     */
    public static <T> RsData<T> of(String resultCode, String msg, T data) {

        // null check
        Objects.requireNonNull(resultCode, "resultCode는 null이 되면 안 됩니다.");
        Objects.requireNonNull(msg, "msg는 null이 되면 안 됩니다");
        Objects.requireNonNull(data, "data는 null이 되면 안 됩니다");

        int statusCode = parseStatusCode(resultCode);
        return new RsData<>(resultCode, statusCode, msg, data);
    }

    /**
     * 기존 resultCode/msg/statusCode를 유지하면서 data만 교체하는 메서드
     * @param base 기존의 인스턴스
     * @param data 기존 인스턴스에 새롭게 넣을 데이터
     * @return 새롭게 생성된 인스턴스
     * @param <T> 새로 주입하는 페이로드의 타입
     */
    public static <T> RsData<T> newDataOf(RsData<?> base, T data) {

        // null check
        Objects.requireNonNull(base, "base RsData는 null이 되면 안 됩니다");
        Objects.requireNonNull(data, "data는 null이 되면 안 됩니다");

        return new RsData<>(base.getResultCode(), base.getStatusCode(), base.getMsg(), data);
    }

    // ======================= 상태 판별 메서드 =========================

    /**
     * 상태 판별 메서드 : 결과 코드에서 상태 코드를 반환하는 메서드
     * @param resultCode 결과 코드 (상태 코드 + 세부 코드)
     * @return 결과 코드에서 추출한 "상태 코드"
     */
    private static int parseStatusCode(String resultCode) {

        // result code null 체크
        if (resultCode == null) {
            throw new IllegalArgumentException("resultCode is null");
        }

        // "-"를 기준으로 최대 2개로 분리
        String[] parts = resultCode.split("-", 2);

        // 정수 파싱 불가 시, 에러 반환
        try {
            return Integer.parseInt(parts[0]);
        } catch (Exception e) {
            throw new IllegalArgumentException("유효하지 않은 결과 코드입니다 : " + resultCode + ". 다음과 같은 형식을 요구합니다 :'200-1'", e);
        }
    }

}