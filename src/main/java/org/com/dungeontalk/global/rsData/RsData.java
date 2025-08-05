package org.com.dungeontalk.global.rsData;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

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
}