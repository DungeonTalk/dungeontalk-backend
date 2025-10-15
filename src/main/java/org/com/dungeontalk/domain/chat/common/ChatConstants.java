package org.com.dungeontalk.domain.chat.common;

public final class ChatConstants {

    private ChatConstants() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    // WebSocket Destinations
    public static final String WEBSOCKET_DESTINATION_PREFIX = "/sub/chat/room/";

    // 세션 키 접두사
    public static final String CHAT_SESSION_PREFIX = "chat:session:";
    public static final String CHAT_ROOM_MEMBERS_PREFIX = "chat:room:";
    public static final String CHAT_SESSION_LOCK_PREFIX = "chat:session:lock:";

    // 기본 타임아웃 설정 (세션 관리)
    public static final int DEFAULT_SESSION_TIMEOUT_SECONDS = 1800; // 30분
    public static final int DEFAULT_SESSION_HEARTBEAT_SECONDS = 300; // 5분 (heartbeat 주기)
    public static final int DEFAULT_SESSION_LOCK_TIMEOUT_SECONDS = 60; // 1분

    // 세션 상태
    public static final String SESSION_STATUS_ACTIVE = "ACTIVE";
    public static final String SESSION_STATUS_IDLE = "IDLE";
    public static final String SESSION_STATUS_DISCONNECTED = "DISCONNECTED";

    // 시스템 메시지 발신자
    public static final String SYSTEM_SENDER_ID = "SYSTEM";
    public static final String SYSTEM_SENDER_NICKNAME = "시스템";

    // Presence 이벤트 타입
    public static final String EVENT_JOIN = "JOIN";
    public static final String EVENT_LEAVE = "LEAVE";
    public static final String EVENT_JOIN_IGNORED = "JOIN_IGNORED";
    public static final String EVENT_LEAVE_IGNORED = "LEAVE_IGNORED";
    public static final String EVENT_CAPACITY_UPDATED = "CAPACITY_UPDATED";
    public static final String EVENT_SESSION_EXPIRED = "SESSION_EXPIRED";

    // 정리 작업 설정
    public static final int INACTIVE_SESSION_CLEANUP_HOURS = 24; // 24시간 이상 비활성 세션 정리
}
