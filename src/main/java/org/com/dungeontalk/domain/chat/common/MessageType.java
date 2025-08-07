package org.com.dungeontalk.domain.chat.common;

public enum MessageType {
    JOIN, TALK, LEAVE, CONNECTED_COUNT;

    // @Enumerated 반영 안 되는 이슈 처리
    @Override
    public String toString() {
        return name();  // name() == "JOIN", "TALK" 등
    }
}
