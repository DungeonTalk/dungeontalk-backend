package org.com.dungeontalk.global.rsData;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

import java.util.Objects;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RsData<T> {

    @NonNull
    private String resultCode; // status + 세부 코드 => ex: "200-1"

    @NonNull
    private int statusCode;

    @NonNull
    private String msg;

    @NonNull
    private T data; //  payload

    // ======================= 기본 상수 =========================

    public static final RsData<Empty> OK = of("200-1", "성공", new Empty());
    public static final RsData<Empty> FAIL = of("500-1", "실패", new Empty());

    // ======================= RsData 생성 메서드 =========================

    /**
     * (메인) 결과코드, 메시지, 데이터를 통해 응답 객체를 반환하는 메서드
     * @param resultCode 결과 코드
     * @param msg 메시지
     * @param data 데이터
     * @return 응답 객체
     * @param <T> 페이로드의 타입
     */
    public static <T> RsData<T> of(String resultCode, String msg, T data) {
        Objects.requireNonNull(resultCode, "resultCode는 null이 되면 안 됩니다.");
        Objects.requireNonNull(msg, "msg는 null이 되면 안 됩니다");
        Objects.requireNonNull(data, "data는 null이 되면 안 됩니다");

        int statusCode = parseStatusCode(resultCode);
        return new RsData<>(resultCode, statusCode, msg, data);
    }

    // 메시지를 통해 응답 객체를 반환 하는 메서드
    public static RsData<Empty> of(String msg) {
        return of("200-1", msg, new Empty());
    }

    // 데이터를 통해 응답 객체를 반환하는 메서드
    public static <T> RsData<T> of(T data) {
        return of("200-1", "성공", data);
    }

    // 메시지, 데이터를 통해 응답 객체를 반환하는 메서드
    public static <T> RsData<T> of(String msg, T data) {
        return of("200-1", msg, data);
    }

    // 결과 코드, 메시지를 통해 응답 객체를 반환하는 메서드
    @SuppressWarnings("unchecked")
    public static <T> RsData<T> of(String resultCode, String msg) {
        return of(resultCode, msg, (T) new Empty());
    }

    // 기존의 응답 객체에서 개로운 데이터를 주입하는 메서드
    public static <T> RsData<T> newDataOf(RsData<?> base, T data) {
        Objects.requireNonNull(base, "base RsData는 null이 되면 안 됩니다");
        Objects.requireNonNull(data, "data는 null이 되면 안 됩니다");

        return new RsData<>(base.getResultCode(), base.getStatusCode(), base.getMsg(), data);
    }

    // ======================= 상태 판별 메서드 =========================

    /**
     * 상태 판별 메서드(resultCode = 상태코드 + 세부 코드 이므로, 여기서 삳태코드 추출)
     * @param resultCode 상태코드 + 세부 코드
     * @return 상태 코드
     */
    private static int parseStatusCode(String resultCode) {

        // null check
        if (resultCode == null) {
            throw new IllegalArgumentException("resultCode is null");
        }

        // 슬래시를 기준으로 분할
        String[] parts = resultCode.split("-", 2);

        // Error 반환
        try {
            return Integer.parseInt(parts[0]);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "유효하지 않은 결과 코드입니다 : " + resultCode + ". 다음과 같은 형식을 요구합니다 :'200-1'", e
            );
        }
    }

}