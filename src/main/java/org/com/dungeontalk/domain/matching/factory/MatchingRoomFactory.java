package org.com.dungeontalk.domain.matching.factory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.aichat.dto.request.AiGameRoomCreateRequest;
import org.com.dungeontalk.domain.aichat.dto.response.AiGameRoomResponse;
import org.com.dungeontalk.domain.aichat.service.AiGameRoomService;
import org.com.dungeontalk.domain.chat.common.ChatMode;
import org.com.dungeontalk.domain.chat.common.ChatRoomType;
import org.com.dungeontalk.domain.chat.dto.ChatRoomDto;
import org.com.dungeontalk.domain.chat.dto.request.ChatRoomCreateRequestDto;
import org.com.dungeontalk.domain.chat.service.ChatRoomService;
import org.com.dungeontalk.domain.matching.common.MatchingConstants;
import org.com.dungeontalk.domain.matching.common.WorldType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 매칭 시스템에서 사용되는 방 생성을 담당하는 Factory 클래스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingRoomFactory {
    
    private final AiGameRoomService aiGameRoomService;
    private final ChatRoomService chatRoomService;
    
    /**
     * AI 게임방 생성
     */
    public AiGameRoomResponse createAiGameRoom(RoomCreationContext context) {
        log.debug("AI 게임방 생성 시작: sessionId={}, worldType={}", 
                 context.getGameSessionId(), context.getWorldType());
        
        AiGameRoomCreateRequest request = new AiGameRoomCreateRequest();
        request.setGameId(context.getGameSessionId());
        request.setRoomName(buildAiGameRoomName(context.getWorldType()));
        request.setDescription(buildAiGameRoomDescription(context.getWorldType()));
        request.setMaxParticipants(MatchingConstants.REQUIRED_PARTICIPANTS);
        request.setGameSettings(context.getWorldType().getGameSettings());
        request.setCreatorId(selectCreator(context.getParticipants()));
        
        AiGameRoomResponse response = aiGameRoomService.createAiGameRoom(request);
        
        log.info("AI 게임방 생성 완료: roomId={}, sessionId={}", 
                response.getId(), context.getGameSessionId());
        return response;
    }
    
    /**
     * 채팅방 생성
     */
//    public ChatRoomDto createChatRoom(RoomCreationContext context) {
//        log.debug("채팅방 생성 시작: sessionId={}, worldType={}",
//                 context.getGameSessionId(), context.getWorldType());
//
//        ChatRoomCreateRequestDto request = new ChatRoomCreateRequestDto();
//        request.setRoomName(buildChatRoomName(context.getWorldType()));
//        request.setMode(ChatMode.MULTI);
//        request.setParticipantIds(context.getParticipants());
//
//        ChatRoomDto response = chatRoomService.createRoom(request);
//
//        log.info("채팅방 생성 완료: roomId={}, sessionId={}",
//                response.getId(), context.getGameSessionId());
//        return response;
//    }
    
    /**
     * AI 게임방 이름 생성
     */
    private String buildAiGameRoomName(WorldType worldType) {
        return new StringBuilder()
                .append(worldType.getDisplayName())
                .append(" 랜덤 매칭")
                .toString();
    }
    
    /**
     * AI 게임방 설명 생성
     */
    private String buildAiGameRoomDescription(WorldType worldType) {
        return new StringBuilder()
                .append("랜덤 매칭으로 생성된 ")
                .append(worldType.getDisplayName())
                .append(" 게임방")
                .toString();
    }
    
    /**
     * 채팅방 이름 생성
     */
    private String buildChatRoomName(WorldType worldType) {
        return new StringBuilder()
                .append(worldType.getDisplayName())
                .append(" 채팅방")
                .toString();
    }
    
    /**
     * 방 생성자 선택 (첫 번째 참가자를 생성자로)
     */
    private String selectCreator(List<String> participants) {
        if (participants == null || participants.isEmpty()) {
            throw new IllegalArgumentException("참가자 목록이 비어있습니다");
        }
        return participants.get(0);
    }
}