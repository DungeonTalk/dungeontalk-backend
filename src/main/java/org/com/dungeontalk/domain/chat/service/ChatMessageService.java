package org.com.dungeontalk.domain.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.com.dungeontalk.global.kafka.KafkaPublisher;
import org.com.dungeontalk.global.util.UuidV7Creator;
import org.com.dungeontalk.global.filter.ProfanityFilterService;
import org.com.dungeontalk.global.filter.config.ProfanityFilterProperties;
import org.com.dungeontalk.global.filter.dto.MessageValidationResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;
    private final KafkaPublisher kafkaPublisher;
    private final ObjectMapper objectMapper;
    private final ProfanityFilterService profanityFilterService;
    private final ProfanityFilterProperties profanityFilterProperties;
    private final ChatSessionService chatSessionService;  // 세션 관리 (신규)

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

        // TALK일 때만 브로드캐스트 (비동기 처리)
        if (chatMessageDto != null) {
            kafkaPublisher.publishChatAsync(dto.getRoomId(), chatMessageDto);
        }

        return chatMessageDto;    // chatMessageDto null이면 컨트롤러는 아무 것도 브로드캐스트하지 않음
    }

    /**
     * TALK 메시지 처리 (세션 연장 통합)
     */
    public ChatMessageDto handleTalkMessage(ChatMessageSendRequestDto dto) {
        if (dto.getRoomId() == null || dto.getSenderId() == null
            || dto.getContent() == null || dto.getContent().isBlank()) {
            throw new ChatException(ErrorCode.CHAT_INVALID_PAYLOAD, "roomId/senderId/content required");
        }

        // 세션 연장 (메시지 전송 = 활동)
        chatSessionService.extendSession(dto.getRoomId(), dto.getSenderId());

        // 욕설 필터링 처리 (플레이어 채팅에서 활성화된 경우에만)
        String processedContent = dto.getContent();
        if (profanityFilterProperties.isEnabled() && profanityFilterProperties.isFilterPlayerChat()) {
            MessageValidationResult validation = validateMessage(dto.getContent());
            processedContent = handlePlayerChatProfanityFiltering(dto, validation);

            if (processedContent == null) {
                // BLOCK 모드에서 욕설이 감지되면 null 반환 (메시지 차단)
                return null;
            }
        }

        Member sender = memberRepository.findById(dto.getSenderId())
            .orElseThrow(() -> new ChatException(ErrorCode.CHAT_MEMBER_NOT_FOUND, "senderId=" + dto.getSenderId()));

        ChatMessage message = ChatMessage.builder()
            .messageId(dto.getMessageId() != null ? dto.getMessageId() : UuidV7Creator.create())
            .roomId(dto.getRoomId())
            .senderId(dto.getSenderId())
            .receiverId(dto.getReceiverId())
            .content(processedContent) // 필터링된 내용 사용
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

    /**
     * 메시지 욕설 검증 (플레이어 채팅용)
     */
    private MessageValidationResult validateMessage(String message) {
        if (profanityFilterService.containsProfanity(message)) {
            String filteredMessage = profanityFilterService.filterProfanity(message);
            return MessageValidationResult.profanityDetected(message, filteredMessage);
        }
        return MessageValidationResult.success(message);
    }

    /**
     * 플레이어 채팅 욕설 필터링 정책에 따른 메시지 처리
     */
    private String handlePlayerChatProfanityFiltering(ChatMessageSendRequestDto dto, 
                                                     MessageValidationResult validation) {
        if (!validation.isContainsProfanity()) {
            return dto.getContent(); // 욕설이 없으면 원본 내용 반환
        }

        switch (profanityFilterProperties.getMode()) {
            case BLOCK:
                // 욕설 포함 메시지 차단 및 경고 메시지 전송
                log.warn("플레이어 채팅 욕설 감지로 메시지 차단: roomId={}, userId={}, message={}", 
                        dto.getRoomId(), dto.getSenderId(), dto.getContent());
                sendPlayerChatProfanityWarning(dto.getRoomId(), dto.getSenderId());
                return null; // null 반환으로 메시지 처리 중단

            case FILTER:
                // 욕설을 필터링하여 메시지 교체
                log.info("플레이어 채팅 욕설 필터링 적용: roomId={}, userId={}, original={}, filtered={}", 
                        dto.getRoomId(), dto.getSenderId(), 
                        validation.getOriginalMessage(), validation.getFilteredMessage());
                return validation.getFilteredMessage(); // 필터링된 메시지 반환

            case WARNING:
                // 경고 로그만 남기고 원본 메시지 통과
                log.warn("플레이어 채팅 욕설 감지 (경고만 표시): roomId={}, userId={}, message={}", 
                        dto.getRoomId(), dto.getSenderId(), dto.getContent());
                return dto.getContent(); // 원본 메시지 반환

            default:
                return dto.getContent();
        }
    }

    /**
     * 플레이어 채팅에서 욕설 감지 시 경고 메시지 전송
     */
    private void sendPlayerChatProfanityWarning(String roomId, String userId) {
        try {
            // 시스템 경고 메시지 생성
            ChatMessageSendRequestDto warningDto = ChatMessageSendRequestDto.builder()
                    .messageId(UuidV7Creator.create())
                    .roomId(roomId)
                    .senderId("SYSTEM")
                    .content("⚠️ 부적절한 언어가 감지되어 메시지가 차단되었습니다. 건전한 대화를 부탁드립니다.")
                    .type(MessageType.TALK)
                    .build();

            // 경고 메시지 처리 (재귀 호출 방지를 위해 필터링 비활성화 상태에서 처리)
            ChatMessage warningMessage = ChatMessage.builder()
                    .messageId(warningDto.getMessageId())
                    .roomId(roomId)
                    .senderId("SYSTEM")
                    .content(warningDto.getContent())
                    .type(MessageType.TALK)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            // DB 저장
            chatMessageRepository.save(warningMessage);

            // 경고 메시지 브로드캐스트
            ChatMessageDto warningChatDto = ChatMessageDto.builder()
                    .messageId(warningMessage.getMessageId())
                    .roomId(roomId)
                    .senderId("SYSTEM")
                    .senderNickname("시스템")
                    .content(warningMessage.getContent())
                    .type(MessageType.TALK)
                    .createdAt(warningMessage.getCreatedAt())
                    .build();

            // Kafka를 통해 브로드캐스트 (비동기 처리)
            kafkaPublisher.publishChatAsync(roomId, warningChatDto);
            
        } catch (Exception e) {
            log.error("플레이어 채팅 욕설 경고 메시지 전송 실패: roomId={}, userId={}", roomId, userId, e);
        }
    }

}
