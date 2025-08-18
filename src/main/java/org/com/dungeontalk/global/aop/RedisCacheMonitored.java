package org.com.dungeontalk.global.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisCacheMonitored {

    // 메서드에만 적용 가능
    // @Target(ElementType.METHOD)

    // 실행 중에도 JVM이 어노테이션을 참조 가능
    // @Retention(RetentionPolicy.RUNTIME)
}
