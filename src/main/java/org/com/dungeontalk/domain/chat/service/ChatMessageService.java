package org.com.dungeontalk.domain.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.chat.common.MessageType;
import org.com.dungeontalk.domain.chat.dto.ChatMessageDto;
import org.com.dungeontalk.domain.chat.dto.ConnectedCountMessageDto;
import org.com.dungeontalk.domain.chat.dto.request.ChatMessageSendRequestDto;
import org.com.dungeontalk.domain.chat.dto.response.ChatMessageResponse;
import org.com.dungeontalk.domain.chat.entity.ChatMessage;
import org.com.dungeontalk.domain.chat.entity.ChatRoom;
import org.com.dungeontalk.domain.chat.repository.ChatMessageRepository;
import org.com.dungeontalk.domain.chat.repository.ChatRoomRepository;
import org.com.dungeontalk.domain.member.entity.Member;
import org.com.dungeontalk.domain.member.repository.MemberRepository;
import org.com.dungeontalk.global.redis.ChatRoomMemberManager;
import org.com.dungeontalk.global.redis.RedisPublisher;
import org.com.dungeontalk.global.util.UuidV7Creator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final SimpMessagingTemplate messagingTemplate;

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final RedisPublisher redisPublisher;
    private final ChatRoomMemberManager chatRoomMemberManager;
    private final ObjectMapper objectMapper;

    private static final int MAX_ROOM_CAPACITY = 3;

    /**
     * STOMP 메시지 분기 처리 (Controller에서 단일 호출)
     */
    public ChatMessageDto processMessage(ChatMessageSendRequestDto messageSendRequestDto) throws JsonProcessingException {
        ChatMessageDto chatMessageDto;

        switch (messageSendRequestDto.getType()) {
            case JOIN -> chatMessageDto = handleJoinMessage(messageSendRequestDto);
            case LEAVE -> chatMessageDto = handleLeaveMessage(messageSendRequestDto);
            case TALK -> chatMessageDto = handleTalkMessage(messageSendRequestDto);
            default -> throw new IllegalArgumentException("유효하지 않은 메시지 타입");
        }

        if (messageSendRequestDto.getType().equals("TALK")) {
            ChatMessageDto response = handleTalkMessage(messageSendRequestDto);

            // ✅ 메시지 브로드캐스트 추가
            messagingTemplate.convertAndSend(
                "/sub/chat/room/" + messageSendRequestDto.getRoomId(),
                response
            );
        }

        // 메시지 브로드캐스트
        String json = objectMapper.writeValueAsString(chatMessageDto);
        redisPublisher.publish(messageSendRequestDto.getRoomId(), json);

        // 접속자 수 실시간 브로드캐스트
        broadcastConnectedCount(messageSendRequestDto.getRoomId());

        return chatMessageDto;
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
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        ChatMessage saved = chatMessageRepository.save(message);
        return ChatMessageDto.fromEntity(saved, sender.getNickName());
    }

    /**
     * JOIN 메시지 처리
     */
    public ChatMessageDto handleJoinMessage(ChatMessageSendRequestDto dto) {
        Member sender = getSender(dto);
        String roomId = dto.getRoomId();
        String nickName = sender.getNickName();

        // Redis 인원 제한 확인
        Set<String> currentUsers = chatRoomMemberManager.getUserList(roomId);
        if (currentUsers.size() >= MAX_ROOM_CAPACITY) {
            throw new IllegalStateException("채팅방 인원이 가득 찼습니다.");
        }

        // Redis에 접속자 등록
        chatRoomMemberManager.addUser(roomId, nickName);

        // MongoDB에 참여자 목록 추가
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("채팅방 없음"));
        if (!room.getParticipants().contains(dto.getSenderId())) {
            room.getParticipants().add(dto.getSenderId());
            chatRoomRepository.save(room);
        }

        // 시스템 메시지 설정
        dto.setSenderNickname(nickName);
        dto.setContent(nickName + "님이 입장했습니다.");
        dto.setType(MessageType.JOIN);

        ChatMessage message = ChatMessage.builder()
            .messageId(UuidV7Creator.create())
            .roomId(roomId)
            .senderId(dto.getSenderId())
            .content(dto.getContent())
            .type(MessageType.JOIN)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        ChatMessage saved = chatMessageRepository.save(message);
        return ChatMessageDto.fromEntity(saved, nickName);
    }

    /**
     * LEAVE 메시지 처리
     */
    public ChatMessageDto handleLeaveMessage(ChatMessageSendRequestDto dto) {
        Member sender = getSender(dto);
        String roomId = dto.getRoomId();
        String nickName = sender.getNickName();

        // Redis 접속자 제거
        chatRoomMemberManager.removeUser(roomId, nickName);

        // MongoDB 참여자 목록 제거
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("채팅방 없음"));
        if (room.getParticipants().contains(dto.getSenderId())) {
            room.getParticipants().remove(dto.getSenderId());
            chatRoomRepository.save(room);
        }

        // 시스템 메시지 설정
        dto.setSenderNickname(nickName);
        dto.setContent(nickName + "님이 퇴장했습니다.");
        dto.setType(MessageType.LEAVE);

        ChatMessage message = ChatMessage.builder()
            .messageId(UuidV7Creator.create())
            .roomId(roomId)
            .senderId(dto.getSenderId())
            .content(dto.getContent())
            .type(MessageType.LEAVE)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        ChatMessage saved = chatMessageRepository.save(message);
        return ChatMessageDto.fromEntity(saved, nickName);
    }

    /**
     * 실시간 접속자 수 브로드캐스트
     */
    public void broadcastConnectedCount(String roomId) {
        try {
            long count = chatRoomMemberManager.getUserCount(roomId);

            ConnectedCountMessageDto broadcastMsg = ConnectedCountMessageDto.builder()
                .roomId(roomId)
                .connectedCount(count)
                .type(MessageType.CONNECTED_COUNT)
                .build();

            String json = objectMapper.writeValueAsString(broadcastMsg);
            redisPublisher.publish(roomId, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("접속자 수 전송 실패", e);
        }
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
