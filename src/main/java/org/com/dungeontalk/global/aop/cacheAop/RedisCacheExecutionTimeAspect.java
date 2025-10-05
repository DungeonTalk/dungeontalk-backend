package org.com.dungeontalk.global.aop.cacheAop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RedisCacheExecutionTimeAspect {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheExecutionTimeAspect.class);

    @Around("@annotation(org.com.dungeontalk.global.aop.cacheAop.RedisCacheMonitored)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {

        long start = System.nanoTime();

        Object proceed = joinPoint.proceed();

        long executionTime = (System.nanoTime() - start) / 1_000_000;

        log.info("{} executed in {} ms", joinPoint.getSignature(), executionTime);
        return proceed;
    }


}
