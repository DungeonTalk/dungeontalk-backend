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
import org.com.dungeontalk.global.redis.ChatRoomMemberManager;
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
    private final ChatRoomService chatRoomService;;

    /**
     * STOMP 메시지 분기 처리 (Controller에서 단일 호출)
     */
    public ChatMessageDto processMessage(ChatMessageSendRequestDto dto) throws JsonProcessingException {
        ChatMessageDto chatMessageDto = null;

        if (dto.getType() == MessageType.JOIN) {
            // 1) 입장 처리(인원 제한, Mongo/Redis, 접속수 브로드캐스트)
            boolean added = chatRoomService.joinRoom(dto.getRoomId(), dto.getSenderId());
            if (added) {            // ✅ 실제로 추가됐을 때만 시스템 메시지 생성/발행
                chatMessageDto = saveSystemMessage(dto.getRoomId(), dto.getSenderId(), MessageType.JOIN);
            }                       // 중복 입장인 경우 시스템 메시지 생성/발행 안 함 (Presence는 ChatRoomService가 이미 브로드캐스트)
        } else if (dto.getType() == MessageType.LEAVE) {
            boolean removed = chatRoomService.leaveRoom(dto.getRoomId(), dto.getSenderId());
            if (removed) {
                chatMessageDto = saveSystemMessage(dto.getRoomId(), dto.getSenderId(), MessageType.LEAVE);
            } // 중복 퇴장인 경우도 시스템 메시지 생성/발행 안 함
        } else if (dto.getType() == MessageType.TALK) {
            chatMessageDto = handleTalkMessage(dto);
        } else {
            throw new IllegalArgumentException("유효하지 않은 메시지 타입");
        }

        if (chatMessageDto != null) {
            // 메시지 브로드캐스트
            String json = objectMapper.writeValueAsString(chatMessageDto);
            redisPublisher.publish(dto.getRoomId(), json);
        }

        return chatMessageDto;    // chatMessageDto null이면 컨트롤러는 아무 것도 브로드캐스트하지 않음
    }

    /**
     * 메세지 저장
     */
    private ChatMessageDto saveSystemMessage(String roomId, String memberId, MessageType type) {
        String member = memberRepository.findById(memberId)
            .map(Member::getNickName)
            .orElse("알 수 없음");

        String content = (type == MessageType.JOIN)
            ? member + "이 입장했습니다."
            : member + "이 퇴장했습니다.";

        ChatMessage msg = ChatMessage.builder()
            .messageId(UuidV7Creator.create())
            .roomId(roomId)
            .senderId(memberId)
            .content(content)
            .type(type)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        ChatMessage saved = chatMessageRepository.save(msg);
        return ChatMessageDto.fromEntity(saved, member);
    }

    /**
     * TALK 메시지 처리
     */
    public ChatMessageDto handleTalkMessage(ChatMessageSendRequestDto dto) {
        Member sender = getSender(dto);

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
        List<Member> members = memberRepository.findByIdIn(senderIds);
        Map<String, String> senderIdToNicknameMap = members.stream()
            .collect(Collectors.toMap(Member::getId, Member::getNickName));

        // 메시지를 DTO로 변환하면서 senderNickname 매핑
        return messagePage.map(msg -> ChatMessageResponse.builder()
            .id(msg.getMessageId())
            .roomId(msg.getRoomId())
            .senderId(msg.getSenderId())
            .senderNickname(senderIdToNicknameMap.getOrDefault(msg.getSenderId(), "알 수 없음"))
            .message(msg.getContent())
            .createdAt(msg.getCreatedAt())
            .build());
    }

    private Member getSender(ChatMessageSendRequestDto dto) {
        return memberRepository.findById(dto.getSenderId())
            .orElseThrow(() -> new IllegalArgumentException("발신자 정보 없음"));
    }

}
