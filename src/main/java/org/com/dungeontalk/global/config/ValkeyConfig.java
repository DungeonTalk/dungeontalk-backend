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
    @Value("${spring.data.session.host}")
    private String sessionRedisHost;

    @Value("${spring.data.session.port}")
    private int sessionRedisPort;

    // Cache Redis 설정
    @Value("${spring.redis.cache.host}")
    private String cacheRedisHost;

    @Value("${spring.redis.cache.port}")
    private int cacheRedisPort;


    // ======================= Redis Basic Config =========================

    // 기본적인 Redis 탬플릿
    @Bean(name = "redisTemplate")
    public RedisTemplate<String, String> redisTemplate(
            @Qualifier("sessionRedisConnectionFactory") RedisConnectionFactory connectionFactory) {
        return sessionRedisTemplate(connectionFactory); // 재사용
    }

    // ======================= Session Redis =========================

    // Session(Valkey) 연결용 팩토리
    @Bean(name = "sessionRedisConnectionFactory")
    @Primary
    public RedisConnectionFactory sessionRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(sessionRedisHost);
        config.setPort(sessionRedisPort);
        // config.setPassword(sessionRedisPassword);
        return new LettuceConnectionFactory(config);
    }

    // Session(Valkey) 저장용 템플릿
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

    // cache(Valkey) 연결용 팩토리
    @Bean(name = "cacheRedisConnectionFactory")
    public RedisConnectionFactory cacheRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(cacheRedisHost);
        config.setPort(cacheRedisPort);
        //config.setPassword(cacheRedisPassword);
        return new LettuceConnectionFactory(config);
    }

    // Cache(Valkey) 저장용 템플릿
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