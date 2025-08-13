package org.com.dungeontalk.domain.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.chat.common.MessageType;
import org.com.dungeontalk.domain.chat.dto.ChatMessageDto;
import org.com.dungeontalk.domain.chat.dto.request.ChatMessageSendRequestDto;
import org.com.dungeontalk.domain.chat.dto.response.ChatMessageResponse;
import org.com.dungeontalk.domain.chat.entity.ChatMessage;
import org.com.dungeontalk.domain.chat.repository.ChatMessageRepository;
import org.com.dungeontalk.domain.member.entity.Member;
import org.com.dungeontalk.domain.member.repository.MemberRepository;
import org.com.dungeontalk.global.exception.ErrorCode;
import org.com.dungeontalk.global.exception.customException.ChatException;
import org.com.dungeontalk.global.redis.RedisPublisher;
import org.com.dungeontalk.global.util.UuidV7Creator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;
    private final RedisPublisher redisPublisher;
    private final ObjectMapper objectMapper;

    // 참여자/인원/브로드캐스트는 ChatRoomService에 위임
    private final ChatRoomService chatRoomService;

    /**
     * STOMP 메시지 분기 처리 (Controller에서 단일 호출)
     */
    public ChatMessageDto processMessage(ChatMessageSendRequestDto dto) throws JsonProcessingException {
        if (dto == null) {
            throw new ChatException(ErrorCode.CHAT_INVALID_PAYLOAD, "payload=null");
        }

        if (dto.getType() == null) {
            throw new ChatException(ErrorCode.CHAT_INVALID_MESSAGE_TYPE, "type=null");
        }

        ChatMessageDto chatMessageDto = null;

        switch (dto.getType()) {
            // 정원 체크 + 입장(멱등)
            case JOIN -> chatRoomService.joinRoom(dto.getRoomId(), dto.getSenderId());   // 예외는 하위에서 throw
            case LEAVE -> chatRoomService.leaveRoom(dto.getRoomId(), dto.getSenderId());
            case TALK -> chatMessageDto = handleTalkMessage(dto);
            case PRESENCE -> { return null; }       // 클라이언트가 직접 PRESENCE를 보낼 일은 없음(보완)
            default -> throw new ChatException(ErrorCode.CHAT_INVALID_MESSAGE_TYPE, "type=" + dto.getType());
        }

        // TALK일 때만 브로드캐스트
        if (chatMessageDto != null) {
            redisPublisher.publish(dto.getRoomId(), objectMapper.writeValueAsString(chatMessageDto));
        }

        return chatMessageDto;    // chatMessageDto null이면 컨트롤러는 아무 것도 브로드캐스트하지 않음
    }

    /**
     * TALK 메시지 처리
     */
    public ChatMessageDto handleTalkMessage(ChatMessageSendRequestDto dto) {
        if (dto.getRoomId() == null || dto.getSenderId() == null
            || dto.getContent() == null || dto.getContent().isBlank()) {
            throw new ChatException(ErrorCode.CHAT_INVALID_PAYLOAD, "roomId/senderId/content required");
        }

        Member sender = memberRepository.findById(dto.getSenderId())
            .orElseThrow(() -> new ChatException(ErrorCode.CHAT_MEMBER_NOT_FOUND, "senderId=" + dto.getSenderId()));

        ChatMessage message = ChatMessage.builder()
            .messageId(dto.getMessageId() != null ? dto.getMessageId() : UuidV7Creator.create())
            .roomId(dto.getRoomId())
            .senderId(dto.getSenderId())
            .receiverId(dto.getReceiverId())
            .content(dto.getContent())
            .type(MessageType.TALK)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        ChatMessage saved = chatMessageRepository.save(message);
        return ChatMessageDto.fromEntity(saved, sender.getNickName());
    }

    /**
     * 채팅방 내 메시지 페이징 조회
     */
    public Page<ChatMessageResponse> getMessagesByRoomId(String roomId, Pageable pageable) {
        Page<ChatMessage> messagePage = chatMessageRepository.findByRoomId(roomId, pageable);

        // senderId 목록 추출
        List<String> senderIds = messagePage.getContent().stream()
            .map(ChatMessage::getSenderId)
            .distinct()
            .collect(Collectors.toList());

        // PostgreSQL에서 senderId로 회원 닉네임 조회
        Map<String, String> idToNick = memberRepository.findByIdIn(senderIds).stream()
            .collect(Collectors.toMap(Member::getId, Member::getNickName));

        // 메시지를 DTO로 변환하면서 senderNickname 매핑
        return messagePage.map(msg -> ChatMessageResponse.builder()
            .id(msg.getMessageId())
            .roomId(msg.getRoomId())
            .senderId(msg.getSenderId())
            .senderNickname(idToNick.getOrDefault(msg.getSenderId(), "알 수 없음"))
            .content(msg.getContent())           // 응답 DTO는 content로 일치
            .createdAt(msg.getCreatedAt())
            .build());
    }

}
