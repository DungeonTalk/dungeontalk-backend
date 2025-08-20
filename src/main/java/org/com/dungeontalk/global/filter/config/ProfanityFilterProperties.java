package org.com.dungeontalk.global.filter.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 욕설 필터링 설정 Properties
 */
@Component
@ConfigurationProperties(prefix = "profanity-filter")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfanityFilterProperties {

    /**
     * 욕설 필터링 활성화 여부
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * 필터링 모드
     * BLOCK: 욕설 포함 시 메시지 차단
     * FILTER: 욕설을 대체 문자로 변경
     * WARNING: 경고만 표시하고 통과
     */
    @Builder.Default
    private FilterMode mode = FilterMode.FILTER;

    /**
     * 욕설 대체 문자
     */
    @Builder.Default
    private String replacement = "***";

    /**
     * 공백 무시 검사 활성화 여부
     */
    @Builder.Default
    private boolean ignoreSpaces = true;

    /**
     * AI 채팅에서 욕설 필터링 활성화 여부
     */
    @Builder.Default
    private boolean filterAiChat = true;

    /**
     * 플레이어 채팅에서 욕설 필터링 활성화 여부
     */
    @Builder.Default
    private boolean filterPlayerChat = true;

    /**
     * 관리자 메시지에서 욕설 필터링 활성화 여부
     */
    @Builder.Default
    private boolean filterAdminMessages = false;

    public enum FilterMode {
        BLOCK,      // 차단
        FILTER,     // 필터링
        WARNING     // 경고
    }
}