package org.com.dungeontalk.domain.matching.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 매칭 도메인 비동기 처리 설정
 */
@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class MatchingAsyncConfig {

    private final MatchingProperties matchingProperties;

    /**
     * 매칭 처리 전용 스레드 풀
     */
    @Bean(name = "matchingTaskExecutor")
    public Executor matchingTaskExecutor() {
        MatchingProperties.ThreadPool config = matchingProperties.getThreadPool();
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(config.getCorePoolSize());
        executor.setMaxPoolSize(config.getMaxPoolSize());
        executor.setQueueCapacity(config.getQueueCapacity());
        executor.setKeepAliveSeconds(config.getKeepAliveSeconds());
        executor.setThreadNamePrefix("Matching-Async-");
        executor.setRejectedExecutionHandler((runnable, threadPoolExecutor) -> {
            log.warn("매칭 처리 스레드 풀이 가득 참. 요청 거부됨");
            // 메인 스레드에서 동기 실행
            runnable.run();
        });
        executor.initialize();
        
        log.info("매칭 전용 스레드 풀 초기화 완료: core={}, max={}, queue={}", 
                config.getCorePoolSize(), config.getMaxPoolSize(), config.getQueueCapacity());
        
        return executor;
    }
}