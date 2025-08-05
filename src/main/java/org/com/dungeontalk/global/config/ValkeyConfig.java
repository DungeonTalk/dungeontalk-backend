package org.com.dungeontalk.global.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class ValkeyConfig {

    // Session Redis 설정
    @Value("${spring.data.redis.host}")
    private String sessionRedisHost;

    @Value("${spring.data.redis.port}")
    private int sessionRedisPort;

    // Cache Redis 설정
    @Value("${spring.redis.cache.host}")
    private String cacheRedisHost;

    @Value("${spring.redis.cache.port}")
    private int cacheRedisPort;


    // ======================= Redis Basic Config =========================

    /**
     * 기본 이름의 redisTemplate
     * <p>
     * description : RedisTemplate의 기본 이름이 필요한 외부 또는 내부 컴포넌트를 에러 없이 동작시키기 위해 사용
     *
     * @param connectionFactory
     * @return
     */
    @Bean(name = "redisTemplate")
    public RedisTemplate<String, String> redisTemplate(
            @Qualifier("sessionRedisConnectionFactory") RedisConnectionFactory connectionFactory) {
        return sessionRedisTemplate(connectionFactory); // 재사용
    }

    // ======================= Session Redis =========================

    /**
     * Session Redis Connection Factory
     *
     * @return
     */
    @Bean(name = "sessionRedisConnectionFactory")
    @Primary
    public RedisConnectionFactory sessionRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(sessionRedisHost);
        config.setPort(sessionRedisPort);
        // config.setPassword(sessionRedisPassword);
        return new LettuceConnectionFactory(config);
    }

    /**
     * Session Redis Template (데이터 저장용)
     *
     * @param connectionFactory
     * @return
     */
    @Bean(name = "sessionRedisTemplate")
    public RedisTemplate<String, String> sessionRedisTemplate(
            @Qualifier("sessionRedisConnectionFactory") RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());

        return template;
    }

    // ======================= Cache Redis =========================

    /**
     * Cache Redis Connection Factory
     *
     * @return
     */
    @Bean(name = "cacheRedisConnectionFactory")
    public RedisConnectionFactory cacheRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(cacheRedisHost);
        config.setPort(cacheRedisPort);
        //config.setPassword(cacheRedisPassword);
        return new LettuceConnectionFactory(config);
    }

    /**
     * Cache Redis Template - redis를 직접 다룰 때 사용
     *
     * @param connectionFactory
     * @return
     */
    @Bean(name = "cacheRedisTemplate")
    public RedisTemplate<String, String> cacheRedisTemplate(
            @Qualifier("cacheRedisConnectionFactory") RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}