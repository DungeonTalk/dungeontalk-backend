package org.com.dungeontalk.domain.chat.config;

import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Chat 도메인 전용 비동기 처리 설정
 * - 일반 채팅 메시지 처리용 스레드 풀
 * - Redis Pub/Sub 비동기 처리용 스레드 풀
 * - Kafka 메시지 발행 비동기 처리용 스레드 풀
 * - Event Listener 비동기 처리용 스레드 풀
 */
@Slf4j
@Configuration
@EnableAsync
public class ChatAsyncConfig {

    /**
     * 채팅 메시지 처리용 스레드 풀
     * - DB 저장 및 조회 작업
     * - 욕설 필터링 등 메시지 전처리
     */
    @Bean(name = "chatMessageExecutor")
    public Executor chatMessageExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("Chat-Message-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        // 큐가 가득 찰 경우, 호출 스레드에서 직접 실행 (CallerRunsPolicy)
        executor.setRejectedExecutionHandler((runnable, threadPoolExecutor) -> {
            log.warn("채팅 메시지 실행자 큐가 가득 찼습니다. 호출 스레드에서 작업을 실행합니다.");
            runnable.run();
        });

        executor.initialize();
        log.info("chatMessageExecutor 초기화 완료: core={}, max={}, queue={}",
            executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * Redis Pub/Sub 처리용 스레드 풀
     * - Redis 메시지 발행 비동기 처리
     * - Redis 작업 부하를 메인 스레드에서 분리
     */
    @Bean(name = "chatRedisExecutor")
    public Executor chatRedisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("Chat-Redis-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.setRejectedExecutionHandler((runnable, threadPoolExecutor) -> {
            log.warn("채팅 Redis 실행자 큐가 가득 찼습니다. 호출 스레드에서 작업을 실행합니다.");
            runnable.run();
        });

        executor.initialize();
        log.info("chatRedisExecutor 초기화 완료: core={}, max={}, queue={}",
            executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * Kafka 메시지 발행용 스레드 풀
     * - Kafka 메시지 발행 비동기 처리
     * - Kafka 작업 부하를 메인 스레드에서 분리
     */
    @Bean(name = "chatKafkaExecutor")
    public Executor chatKafkaExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(15);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("Chat-Kafka-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.setRejectedExecutionHandler((runnable, threadPoolExecutor) -> {
            log.warn("채팅 Kafka 실행자 큐가 가득 찼습니다. 호출 스레드에서 작업을 실행합니다.");
            runnable.run();
        });

        executor.initialize();
        log.info("chatKafkaExecutor 초기화 완료: core={}, max={}, queue={}",
            executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * 이벤트 리스너 처리용 스레드 풀
     * - ChatPresenceEvent 등 도메인 이벤트 처리
     * - 시스템 메시지 생성 및 브로드캐스트
     */
    @Bean(name = "chatEventExecutor")
    public Executor chatEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(15);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("Chat-Event-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.setRejectedExecutionHandler((runnable, threadPoolExecutor) -> {
            log.warn("채팅 이벤트 실행자 큐가 가득 찼습니다. 호출 스레드에서 작업을 실행합니다.");
            runnable.run();
        });

        executor.initialize();
        log.info("chatEventExecutor 초기화 완료: core={}, max={}, queue={}",
            executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }
}
