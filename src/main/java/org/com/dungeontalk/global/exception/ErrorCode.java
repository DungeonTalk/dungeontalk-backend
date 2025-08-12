package org.com.dungeontalk.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 템플릿 : 상태 코드 - [에러가 난 도메인]+[세부 숫자]
    // 상태 코드는 RsData에서 추출 ?  혹은 여기서 명시?

    /* 클라이언트 에러 */
    INVALID_JWT_TOKEN("401-AU01", "유효하지 않은 JWT 토큰입니다."),
    EXPIRED_JWT_TOKEN("401-AU02", "만료된 JWT 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND("401-AU03", "저장소에 존재하지 않는 리프레시 토큰입니다."),

    /* 예시 : 5xx */
    GLOBAL_ERROR("500-GL01", "서버 오류"),
    DATABASE_ERROR("500-DB01","데이터 베이스 오류"),

    /* AI Chat 관련 : 4xx */
    // AI 게임방 관련
    AI_GAME_ROOM_NOT_FOUND("404-AC01", "AI 게임방을 찾을 수 없습니다"),
    AI_GAME_ROOM_CANNOT_JOIN("400-AC02", "입장할 수 없는 게임방입니다"),
    AI_GAME_ROOM_ALREADY_JOINED("400-AC03", "이미 참여중인 게임방입니다"),
    AI_GAME_ROOM_NOT_PARTICIPATING("400-AC04", "참여하지 않은 게임방입니다"),
    AI_GAME_ROOM_INVALID_STATE("400-AC05", "잘못된 게임 상태입니다"),
    AI_GAME_ROOM_INVALID_PHASE("400-AC06", "잘못된 게임 단계입니다"),

    // AI 게임 메시지 관련
    AI_GAME_MESSAGE_INVALID_STATE("400-AC11", "메시지를 보낼 수 없는 상태입니다"),
    AI_GAME_MESSAGE_ROOM_NOT_FOUND("404-AC12", "메시지를 보낼 게임방을 찾을 수 없습니다"),

    // 멤버 검증 관련
    MEMBER_NOT_FOUND("404-AC21", "사용자 정보를 찾을 수 없습니다"),

    // AI 응답 처리 관련
    AI_RESPONSE_PROCESSING_ERROR("500-AC31", "AI 응답 처리 중 오류가 발생했습니다"),
    AI_RESPONSE_TIMEOUT_ERROR("408-AC32", "AI 응답 시간이 초과되었습니다"),

    /* 매칭 관련 : 4xx */
    MATCHING_USER_ALREADY_IN_QUEUE("400-MT01", "이미 매칭 대기 중입니다"),
    MATCHING_QUEUE_FULL("400-MT02", "매칭 대기열이 가득 찼습니다"),
    MATCHING_USER_NOT_IN_QUEUE("404-MT03", "매칭 대기 중이 아닙니다"),
    MATCHING_PROCESSING_ERROR("500-MT04", "매칭 처리 중 오류가 발생했습니다");



    private final String errorCode;
    private final String message;

}
