package org.com.dungeontalk.domain.aichat.util;

import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.aichat.repository.AiGameRoomRepository;
import org.com.dungeontalk.domain.member.repository.MemberRepository;
import org.com.dungeontalk.global.exception.ErrorCode;
import org.com.dungeontalk.global.exception.customException.AiChatException;
import org.springframework.stereotype.Component;

/**
 * AI 게임 관련 공통 검증 로직을 담당하는 유틸리티 클래스
 */
@Component
@RequiredArgsConstructor
public class AiGameValidator {

    private final AiGameRoomRepository aiGameRoomRepository;
    private final MemberRepository memberRepository;

    /**
     * AI 게임방 존재 여부 검증
     * @param aiGameRoomId 게임방 ID
     * @throws AiChatException 게임방이 존재하지 않는 경우
     */
    public void validateGameRoom(String aiGameRoomId) {
        if (!aiGameRoomRepository.existsById(aiGameRoomId)) {
            throw new AiChatException(ErrorCode.AI_GAME_ROOM_NOT_FOUND);
        }
    }

    /**
     * 발신자(멤버) 존재 여부 검증
     * @param senderId 발신자 ID
     * @throws AiChatException 멤버가 존재하지 않는 경우
     */
    public void validateSender(String senderId) {
        memberRepository.findById(senderId)
                .orElseThrow(() -> new AiChatException(ErrorCode.MEMBER_NOT_FOUND));
    }

    /**
     * 게임방과 발신자 모두 검증
     * @param aiGameRoomId 게임방 ID
     * @param senderId 발신자 ID
     */
    public void validateGameRoomAndSender(String aiGameRoomId, String senderId) {
        validateGameRoom(aiGameRoomId);
        validateSender(senderId);
    }
}