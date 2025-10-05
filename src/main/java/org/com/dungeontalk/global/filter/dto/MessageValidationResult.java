package org.com.dungeontalk.global.filter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 메시지 검증 결과 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageValidationResult {

    /**
     * 메시지가 유효한지 여부
     */
    private boolean valid;

    /**
     * 욕설이 포함되어 있는지 여부
     */
    private boolean containsProfanity;

    /**
     * 검증 실패 사유
     */
    private String reason;

    /**
     * 원본 메시지
     */
    private String originalMessage;

    /**
     * 필터링된 메시지
     */
    private String filteredMessage;

    /**
     * 검증 유형
     */
    private ValidationType validationType;

    public enum ValidationType {
        PROFANITY_FILTER,    // 욕설 필터링
        LENGTH_CHECK,        // 길이 체크
        SPAM_CHECK,          // 스팸 체크
        CUSTOM_RULE          // 커스텀 규칙
    }

    /**
     * 성공 결과 생성
     */
    public static MessageValidationResult success(String message) {
        return MessageValidationResult.builder()
                .valid(true)
                .containsProfanity(false)
                .originalMessage(message)
                .filteredMessage(message)
                .build();
    }

    /**
     * 욕설 필터링 실패 결과 생성
     */
    public static MessageValidationResult profanityDetected(String originalMessage, String filteredMessage) {
        return MessageValidationResult.builder()
                .valid(false)
                .containsProfanity(true)
                .reason("부적절한 언어가 포함되어 있습니다")
                .originalMessage(originalMessage)
                .filteredMessage(filteredMessage)
                .validationType(ValidationType.PROFANITY_FILTER)
                .build();
    }

    /**
     * 필터링된 메시지 허용 결과 생성 (욕설을 별표로 치환해서 허용)
     */
    public static MessageValidationResult filteredSuccess(String originalMessage, String filteredMessage) {
        return MessageValidationResult.builder()
                .valid(true)
                .containsProfanity(true)
                .originalMessage(originalMessage)
                .filteredMessage(filteredMessage)
                .validationType(ValidationType.PROFANITY_FILTER)
                .build();
    }
}