package org.com.dungeontalk.domain.aichat.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

/**
 * AI Chat 설정값들을 쉽게 접근할 수 있게 도와주는 Helper 클래스
 * 동료 개발자들이 @Autowired 없이도 설정값을 사용할 수 있도록 정적 메서드 제공
 */
@Component
@RequiredArgsConstructor
public class AiChatConfigHelper {

    private final AiChatProperties properties;
    
    private static AiChatConfigHelper instance;

    // Spring Bean 생성 후 정적 접근을 위한 인스턴스 설정
    private void init() {
        instance = this;
    }

    /**
     * 세션 타임아웃 (초)
     */
    public static int getSessionTimeoutSeconds() {
        return instance != null ? instance.properties.getSession().getTimeoutSeconds() : 3600;
    }

    /**
     * 턴 락 타임아웃 (초)
     */
    public static int getTurnLockTimeoutSeconds() {
        return instance != null ? instance.properties.getSession().getTurnLockTimeoutSeconds() : 300;
    }

    /**
     * AI 컨텍스트 메시지 개수
     */
    public static int getContextMessageCount() {
        return instance != null ? instance.properties.getContext().getMessageCount() : 5;
    }

    /**
     * WebSocket destination prefix
     */
    public static String getWebSocketDestinationPrefix() {
        return instance != null ? instance.properties.getWebsocket().getDestinationPrefix() : "/sub/aichat/room/";
    }

    /**
     * 턴 시작 메시지 순서
     */
    public static int getTurnStartMessageOrder() {
        return instance != null ? instance.properties.getMessageOrder().getTurnStart() : 0;
    }

    /**
     * 턴 종료 메시지 순서
     */
    public static int getTurnEndMessageOrder() {
        return instance != null ? instance.properties.getMessageOrder().getTurnEnd() : 9999;
    }

    /**
     * 에러 메시지 순서
     */
    public static int getErrorMessageOrder() {
        return instance != null ? instance.properties.getMessageOrder().getError() : 9998;
    }

    // Spring Bean 생성 시점에 정적 인스턴스 설정
    @PostConstruct
    private void postConstruct() {
        init();
    }
}