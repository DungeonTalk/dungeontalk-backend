package org.com.dungeontalk.domain.aichat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.aichat.common.AiGamePhase;
import org.com.dungeontalk.domain.aichat.common.AiMessageType;
import org.com.dungeontalk.domain.aichat.dto.AiGameMessageDto;
import org.com.dungeontalk.domain.aichat.dto.request.AiGameMessageSendRequest;
import org.com.dungeontalk.domain.aichat.dto.response.AiGameMessageResponse;
import org.com.dungeontalk.domain.aichat.entity.AiGameMessage;
import org.com.dungeontalk.domain.aichat.entity.AiGameRoom;
import org.com.dungeontalk.domain.aichat.repository.AiGameMessageRepository;
import org.com.dungeontalk.domain.aichat.repository.AiGameRoomRepository;
import org.com.dungeontalk.domain.member.entity.Member;
import org.com.dungeontalk.domain.member.repository.MemberRepository;
import org.com.dungeontalk.global.redis.RedisPublisher;
import org.com.dungeontalk.global.util.UuidV7Creator;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiGameMessageService {

    private final SimpMessagingTemplate messagingTemplate;
    private final AiGameMessageRepository aiGameMessageRepository;
    private final AiGameRoomRepository aiGameRoomRepository;
    private final MemberRepository memberRepository;
    private final RedisPublisher redisPublisher;
    private final ObjectMapper objectMapper;

    /**
     * STOMP 메시지 분기 처리 (Controller에서 단일 호출)
     */
    @Transactional
    public AiGameMessageDto processMessage(AiGameMessageSendRequest request) throws JsonProcessingException {
        AiGameMessageDto messageDto;

        switch (request.getMessageType()) {
            case USER -> messageDto = handleUserMessage(request);
            case SYSTEM -> messageDto = handleSystemMessage(request);
            case TURN_START -> messageDto = handleTurnStartMessage(request);
            case TURN_END -> messageDto = handleTurnEndMessage(request);
            default -> throw new IllegalArgumentException("지원하지 않는 메시지 타입: " + request.getMessageType());
        }

        // WebSocket 브로드캐스트
        String destination = "/sub/aichat/room/" + request.getAiGameRoomId();
        messagingTemplate.convertAndSend(destination, messageDto);

        // Redis 메시지 브로드캐스트
        String json = objectMapper.writeValueAsString(messageDto);
        redisPublisher.publish(request.getAiGameRoomId(), json);

        return messageDto;
    }

    /**
     * 사용자 메시지 처리
     */
    @Transactional
    public AiGameMessageDto handleUserMessage(AiGameMessageSendRequest request) {
        validateGameRoom(request.getAiGameRoomId());
        validateSender(request.getSenderId());

        AiGameRoom room = aiGameRoomRepository.findById(request.getAiGameRoomId())
                .orElseThrow(() -> new IllegalArgumentException("AI 게임방을 찾을 수 없습니다"));

        // 턴제 검증
        if (!room.getCurrentPhase().equals(AiGamePhase.TURN_INPUT)) {
            throw new IllegalStateException("현재 사용자 입력을 받을 수 없는 상태입니다: " + room.getCurrentPhase());
        }

        // 다음 메시지 순서 계산
        int nextMessageOrder = getNextMessageOrder(request.getAiGameRoomId(), request.getTurnNumber());

        AiGameMessage message = AiGameMessage.builder()
                .id(UuidV7Creator.create())
                .aiGameRoomId(request.getAiGameRoomId())
                .gameId(request.getGameId())
                .senderId(request.getSenderId())
                .senderNickname(request.getSenderNickname())
                .content(request.getContent())
                .messageType(AiMessageType.USER)
                .turnNumber(request.getTurnNumber())
                .messageOrder(nextMessageOrder)
                .createdAt(LocalDateTime.now())
                .build();

        AiGameMessage saved = aiGameMessageRepository.save(message);
        
        // 게임방 마지막 활동 시간 업데이트
        room.setLastActivity(LocalDateTime.now());
        aiGameRoomRepository.save(room);

        log.info("사용자 메시지 저장 완료: roomId={}, sender={}, turn={}", 
                 request.getAiGameRoomId(), request.getSenderId(), request.getTurnNumber());

        return AiGameMessageDto.fromEntity(saved);
    }

    /**
     * AI 메시지 저장 (AI 서비스에서 호출)
     */
    @Transactional
    public AiGameMessageDto saveAiMessage(String aiGameRoomId, String gameId, String content, 
                                         int turnNumber, Long responseTime, String aiSources) {
        validateGameRoom(aiGameRoomId);

        int nextMessageOrder = getNextMessageOrder(aiGameRoomId, turnNumber);

        AiGameMessage message = AiGameMessage.builder()
                .id(UuidV7Creator.create())
                .aiGameRoomId(aiGameRoomId)
                .gameId(gameId)
                .senderId("AI_GM")
                .senderNickname("던전 마스터")
                .content(content)
                .messageType(AiMessageType.AI)
                .turnNumber(turnNumber)
                .messageOrder(nextMessageOrder)
                .aiResponseTime(responseTime)
                .aiSources(aiSources)
                .createdAt(LocalDateTime.now())
                .build();

        AiGameMessage saved = aiGameMessageRepository.save(message);

        // 게임방 마지막 활동 시간 업데이트
        AiGameRoom room = aiGameRoomRepository.findById(aiGameRoomId)
                .orElseThrow(() -> new IllegalArgumentException("AI 게임방을 찾을 수 없습니다"));
        room.setLastActivity(LocalDateTime.now());
        aiGameRoomRepository.save(room);

        log.info("AI 메시지 저장 완료: roomId={}, turn={}, responseTime={}ms", 
                 aiGameRoomId, turnNumber, responseTime);

        return AiGameMessageDto.fromEntity(saved);
    }

    /**
     * 시스템 메시지 처리
     */
    @Transactional
    public AiGameMessageDto handleSystemMessage(AiGameMessageSendRequest request) {
        validateGameRoom(request.getAiGameRoomId());

        AiGameMessage message = AiGameMessage.builder()
                .id(UuidV7Creator.create())
                .aiGameRoomId(request.getAiGameRoomId())
                .gameId(request.getGameId())
                .senderId("SYSTEM")
                .senderNickname("시스템")
                .content(request.getContent())
                .messageType(AiMessageType.SYSTEM)
                .turnNumber(request.getTurnNumber())
                .messageOrder(request.getMessageOrder())
                .createdAt(LocalDateTime.now())
                .build();

        AiGameMessage saved = aiGameMessageRepository.save(message);
        log.info("시스템 메시지 저장 완료: roomId={}, content={}", 
                 request.getAiGameRoomId(), request.getContent());

        return AiGameMessageDto.fromEntity(saved);
    }

    /**
     * 턴 시작 메시지 처리
     */
    @Transactional
    public AiGameMessageDto handleTurnStartMessage(AiGameMessageSendRequest request) {
        validateGameRoom(request.getAiGameRoomId());

        String content = "턴 " + request.getTurnNumber() + " 시작! 플레이어들의 행동을 기다립니다.";

        AiGameMessage message = AiGameMessage.builder()
                .id(UuidV7Creator.create())
                .aiGameRoomId(request.getAiGameRoomId())
                .gameId(request.getGameId())
                .senderId("SYSTEM")
                .senderNickname("시스템")
                .content(content)
                .messageType(AiMessageType.TURN_START)
                .turnNumber(request.getTurnNumber())
                .messageOrder(0) // 턴 시작은 항상 첫 번째
                .createdAt(LocalDateTime.now())
                .build();

        AiGameMessage saved = aiGameMessageRepository.save(message);
        log.info("턴 시작 메시지 저장 완료: roomId={}, turn={}", 
                 request.getAiGameRoomId(), request.getTurnNumber());

        return AiGameMessageDto.fromEntity(saved);
    }

    /**
     * 턴 종료 메시지 처리
     */
    @Transactional
    public AiGameMessageDto handleTurnEndMessage(AiGameMessageSendRequest request) {
        validateGameRoom(request.getAiGameRoomId());

        String content = "턴 " + request.getTurnNumber() + " 완료! 다음 턴을 준비합니다.";

        AiGameMessage message = AiGameMessage.builder()
                .id(UuidV7Creator.create())
                .aiGameRoomId(request.getAiGameRoomId())
                .gameId(request.getGameId())
                .senderId("SYSTEM")
                .senderNickname("시스템")
                .content(content)
                .messageType(AiMessageType.TURN_END)
                .turnNumber(request.getTurnNumber())
                .messageOrder(9999) // 턴 종료는 항상 마지막
                .createdAt(LocalDateTime.now())
                .build();

        AiGameMessage saved = aiGameMessageRepository.save(message);
        log.info("턴 종료 메시지 저장 완료: roomId={}, turn={}", 
                 request.getAiGameRoomId(), request.getTurnNumber());

        return AiGameMessageDto.fromEntity(saved);
    }

    /**
     * AI 게임방 메시지 히스토리 조회 (페이징)
     */
    public List<AiGameMessageResponse> getMessageHistory(String aiGameRoomId, Pageable pageable) {
        validateGameRoom(aiGameRoomId);

        List<AiGameMessage> messages = aiGameMessageRepository
                .findByAiGameRoomIdOrderByCreatedAtDesc(aiGameRoomId, pageable);

        return messages.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 특정 턴의 메시지들 조회
     */
    public List<AiGameMessageResponse> getTurnMessages(String aiGameRoomId, int turnNumber) {
        validateGameRoom(aiGameRoomId);

        List<AiGameMessage> messages = aiGameMessageRepository
                .findByAiGameRoomIdAndTurnNumberOrderByMessageOrder(aiGameRoomId, turnNumber);

        return messages.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * AI 컨텍스트용 최근 턴들의 메시지 조회
     */
    public List<AiGameMessageDto> getContextMessages(String aiGameRoomId, int recentTurnCount, int currentTurn) {
        validateGameRoom(aiGameRoomId);

        int fromTurn = Math.max(1, currentTurn - recentTurnCount + 1);
        List<AiGameMessage> messages = aiGameMessageRepository
                .findRecentTurnsMessages(aiGameRoomId, recentTurnCount, fromTurn);

        return messages.stream()
                .map(AiGameMessageDto::fromEntity)
                .collect(Collectors.toList());
    }

    private void validateGameRoom(String aiGameRoomId) {
        if (!aiGameRoomRepository.existsById(aiGameRoomId)) {
            throw new IllegalArgumentException("AI 게임방을 찾을 수 없습니다: " + aiGameRoomId);
        }
    }

    private void validateSender(String senderId) {
        memberRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("발신자 정보를 찾을 수 없습니다: " + senderId));
    }

    private int getNextMessageOrder(String aiGameRoomId, int turnNumber) {
        List<AiGameMessage> turnMessages = aiGameMessageRepository
                .findMaxMessageOrderByTurn(aiGameRoomId, turnNumber);
        
        if (turnMessages.isEmpty()) {
            return 1;
        }
        
        return turnMessages.stream()
                .mapToInt(AiGameMessage::getMessageOrder)
                .max()
                .orElse(0) + 1;
    }

    private AiGameMessageResponse convertToResponse(AiGameMessage message) {
        return AiGameMessageResponse.builder()
                .messageId(message.getId())
                .aiGameRoomId(message.getAiGameRoomId())
                .senderId(message.getSenderId())
                .senderNickname(message.getSenderNickname())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .turnNumber(message.getTurnNumber())
                .messageOrder(message.getMessageOrder())
                .aiResponseTime(message.getAiResponseTime())
                .createdAt(message.getCreatedAt())
                .build();
    }
}