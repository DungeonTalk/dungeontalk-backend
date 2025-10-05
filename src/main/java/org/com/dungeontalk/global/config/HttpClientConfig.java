package org.com.dungeontalk.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpClientConfig {

    @Value("${ai.service.timeout:60000}")  // 30초 → 60초(1분)로 변경
    private int aiServiceTimeout;

    @Value("${ai.service.connect.timeout:10000}")  // 연결 타임아웃 10초
    private int connectTimeout;

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(clientHttpRequestFactory());
        return restTemplate;
    }

    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);      // 연결 타임아웃 10초
        factory.setReadTimeout(aiServiceTimeout);       // 읽기 타임아웃 60초 (AI 응답 대기)
        return factory;
    }
}