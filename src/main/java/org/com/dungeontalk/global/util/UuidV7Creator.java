package org.com.dungeontalk.global.util;

import com.github.f4b6a3.uuid.UuidCreator;

public class UuidV7Creator {

    public static String create() {
        return UuidCreator.getTimeOrderedEpoch().toString(); // UUIDv7 유사
    }
}
