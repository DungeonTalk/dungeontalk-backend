package org.com.dungeontalk.domain.matching.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MatchingStatus {
    
    WAITING("대기중", "매칭 큐에서 대기중"),
    MATCHED("매칭완료", "매칭이 완료되어 게임방 생성중"),
    COMPLETED("완료", "게임방 생성 완료"),
    CANCELLED("취소", "사용자가 매칭을 취소함"),
    EXPIRED("만료", "대기 시간이 만료됨"),
    ERROR("오류", "매칭 처리 중 오류 발생");

    private final String displayName;
    private final String description;
}