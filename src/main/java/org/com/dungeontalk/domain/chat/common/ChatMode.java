package org.com.dungeontalk.domain.chat.common;

public enum ChatMode {
    SINGLE,
    MULTI;

    // @Enumerated 반영 안 되는 이슈 처리
    @Override
    public String toString() {
        return name();  // name() == "SINGLE", "MULTI" 등
    }
}
