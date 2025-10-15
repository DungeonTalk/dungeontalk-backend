package org.com.dungeontalk.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 로컬 메모리 캐시 설정
 * worldTypes 같은 마스터 데이터를 직렬화 없이 캐싱
 */
@Configuration
@EnableCaching
public class CaffeineCacheConfig {

    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("worldTypes");

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(100) // 최대 100개 항목
                .expireAfterWrite(1, TimeUnit.HOURS) // 1시간 후 만료
                .recordStats()); // 캐시 통계 기록

        return cacheManager;
    }
}
