package org.com.dungeontalk.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 템플릿 : 상태 코드 - [에러가 난 도메인]+[세부 숫자]
    // 상태 코드는 RsData에서 추출 ?  혹은 여기서 명시?

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
    AI_RESPONSE_TIMEOUT_ERROR("408-AC32", "AI 응답 시간이 초과되었습니다");



    private final String errorCode;
    private final String message;

}
