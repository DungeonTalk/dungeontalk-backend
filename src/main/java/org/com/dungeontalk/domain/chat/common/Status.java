package org.com.dungeontalk.domain.chat.common;

public enum Status {
    ONLINE,
    OFFLINE;

    @Override
    public String toString() {
        return name();  // name() == "JOIN", "TALK" 등
    }
}
