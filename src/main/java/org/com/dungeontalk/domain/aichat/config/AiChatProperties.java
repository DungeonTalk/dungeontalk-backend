package org.com.dungeontalk.domain.aichat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI Chat 모듈 설정값들을 외부화한 Properties 클래스
 * application-dev.properties의 aichat.* 설정들을 자동으로 바인딩합니다.
 * 
 * 동료 개발자들이 설정 변경 시 코드 수정 없이 properties 파일만 수정하면 됩니다!
 */
@Data
@Component
@ConfigurationProperties(prefix = "aichat")
public class AiChatProperties {

    /**
     * 세션 관련 설정
     */
    private Session session = new Session();
    
    /**
     * 컨텍스트 관련 설정
     */
    private Context context = new Context();
    
    /**
     * WebSocket 관련 설정
     */
    private Websocket websocket = new Websocket();
    
    /**
     * 메시지 순서 관련 설정
     */
    private MessageOrder messageOrder = new MessageOrder();

    @Data
    public static class Session {
        /**
         * 게임 세션 타임아웃 (초)
         */
        private int timeoutSeconds = 3600;
        
        /**
         * 턴 락 타임아웃 (초)
         */
        private int turnLockTimeoutSeconds = 300;
    }

    @Data
    public static class Context {
        /**
         * AI 컨텍스트 메시지 개수
         */
        private int messageCount = 5;
    }

    @Data
    public static class Websocket {
        /**
         * WebSocket destination prefix
         */
        private String destinationPrefix = "/sub/aichat/room/";
    }

    @Data
    public static class MessageOrder {
        /**
         * 턴 시작 메시지 순서
         */
        private int turnStart = 0;
        
        /**
         * 턴 종료 메시지 순서
         */
        private int turnEnd = 9999;
        
        /**
         * 에러 메시지 순서
         */
        private int error = 9998;
    }
}