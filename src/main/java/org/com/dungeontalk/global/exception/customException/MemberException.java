package org.com.dungeontalk.global.exception.customException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public class MemberException extends RuntimeException {
    private final ErrorCode errorCode;
}
