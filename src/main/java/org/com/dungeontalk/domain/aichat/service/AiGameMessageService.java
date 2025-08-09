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
import org.com.dungeontalk.domain.aichat.util.AiGameValidator;
import org.com.dungeontalk.domain.aichat.service.AiGameRoomService;
import org.com.dungeontalk.global.exception.ErrorCode;
import org.com.dungeontalk.global.exception.customException.AiChatException;
import static org.com.dungeontalk.domain.aichat.common.AiChatConstants.*;
import org.com.dungeontalk.domain.aichat.config.AiChatConfigHelper;
import org.com.dungeontalk.domain.aichat.dto.request.AiMessageSaveRequest;
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
    private final AiGameValidator aiGameValidator;
    private final AiGameRoomService aiGameRoomService;
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
            default -> throw new AiChatException(ErrorCode.AI_GAME_MESSAGE_INVALID_STATE);
        }

        // WebSocket 브로드캐스트
        String destination = WEBSOCKET_DESTINATION_PREFIX + request.getAiGameRoomId();
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
        aiGameValidator.validateGameRoomAndSender(request.getAiGameRoomId(), request.getSenderId());

        AiGameRoom room = aiGameRoomService.getGameRoomEntity(request.getAiGameRoomId());

        // 턴제 검증
        if (!room.getCurrentPhase().equals(AiGamePhase.TURN_INPUT)) {
            throw new AiChatException(ErrorCode.AI_GAME_MESSAGE_INVALID_STATE);
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
     * AI 메시지 저장 (매개변수 객체 패턴 적용)
     */
    @Transactional
    public AiGameMessageDto saveAiMessage(AiMessageSaveRequest request) {
        request.validate();
        aiGameValidator.validateGameRoom(request.getAiGameRoomId());

        int nextMessageOrder = getNextMessageOrder(request.getAiGameRoomId(), request.getTurnNumber());

        AiGameMessage message = AiGameMessage.builder()
                .id(UuidV7Creator.create())
                .aiGameRoomId(request.getAiGameRoomId())
                .gameId(request.getGameId())
                .senderId(AI_SENDER_ID)
                .senderNickname(AI_SENDER_NICKNAME)
                .content(request.getContent())
                .messageType(AiMessageType.AI)
                .turnNumber(request.getTurnNumber())
                .messageOrder(nextMessageOrder)
                .aiResponseTime(request.getResponseTime())
                .aiSources(request.getAiSources())
                .createdAt(LocalDateTime.now())
                .build();

        AiGameMessage saved = aiGameMessageRepository.save(message);

        // 게임방 마지막 활동 시간 업데이트
        updateRoomLastActivity(request.getAiGameRoomId());

        log.info("AI 메시지 저장 완료: roomId={}, turn={}, responseTime={}ms", 
                 request.getAiGameRoomId(), request.getTurnNumber(), request.getResponseTime());

        return AiGameMessageDto.fromEntity(saved);
    }

    /**
     * AI 메시지 저장 (기존 방식 - 호환성을 위해 유지)
     * @deprecated 대신 saveAiMessage(AiMessageSaveRequest)를 사용하세요
     */
    @Deprecated
    @Transactional
    public AiGameMessageDto saveAiMessage(String aiGameRoomId, String gameId, String content, 
                                         int turnNumber, Long responseTime, String aiSources) {
        AiMessageSaveRequest request = AiMessageSaveRequest.builder()
                .aiGameRoomId(aiGameRoomId)
                .gameId(gameId)
                .content(content)
                .turnNumber(turnNumber)
                .responseTime(responseTime)
                .aiSources(aiSources)
                .build();
        
        return saveAiMessage(request);
    }

    /**
     * 시스템 메시지 처리
     */
    @Transactional
    public AiGameMessageDto handleSystemMessage(AiGameMessageSendRequest request) {
        aiGameValidator.validateGameRoom(request.getAiGameRoomId());

        AiGameMessage message = AiGameMessage.builder()
                .id(UuidV7Creator.create())
                .aiGameRoomId(request.getAiGameRoomId())
                .gameId(request.getGameId())
                .senderId(SYSTEM_SENDER_ID)
                .senderNickname(SYSTEM_SENDER_NICKNAME)
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
        aiGameValidator.validateGameRoom(request.getAiGameRoomId());

        String content = String.format(TURN_START_MESSAGE_TEMPLATE, request.getTurnNumber());

        AiGameMessage message = AiGameMessage.builder()
                .id(UuidV7Creator.create())
                .aiGameRoomId(request.getAiGameRoomId())
                .gameId(request.getGameId())
                .senderId(SYSTEM_SENDER_ID)
                .senderNickname(SYSTEM_SENDER_NICKNAME)
                .content(content)
                .messageType(AiMessageType.TURN_START)
                .turnNumber(request.getTurnNumber())
                .messageOrder(AiChatConfigHelper.getTurnStartMessageOrder())
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
        aiGameValidator.validateGameRoom(request.getAiGameRoomId());

        String content = String.format(TURN_END_MESSAGE_TEMPLATE, request.getTurnNumber());

        AiGameMessage message = AiGameMessage.builder()
                .id(UuidV7Creator.create())
                .aiGameRoomId(request.getAiGameRoomId())
                .gameId(request.getGameId())
                .senderId(SYSTEM_SENDER_ID)
                .senderNickname(SYSTEM_SENDER_NICKNAME)
                .content(content)
                .messageType(AiMessageType.TURN_END)
                .turnNumber(request.getTurnNumber())
                .messageOrder(AiChatConfigHelper.getTurnEndMessageOrder())
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
        aiGameValidator.validateGameRoom(aiGameRoomId);

        List<AiGameMessage> messages = aiGameMessageRepository
                .findByAiGameRoomIdOrderByCreatedAtDesc(aiGameRoomId, pageable);

        return messages.stream()
                .map(AiGameMessageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 특정 턴의 메시지들 조회
     */
    public List<AiGameMessageResponse> getTurnMessages(String aiGameRoomId, int turnNumber) {
        aiGameValidator.validateGameRoom(aiGameRoomId);

        List<AiGameMessage> messages = aiGameMessageRepository
                .findTurnMessages(aiGameRoomId, turnNumber);

        return messages.stream()
                .map(AiGameMessageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * AI 컨텍스트용 최근 턴들의 메시지 조회
     */
    public List<AiGameMessageDto> getContextMessages(String aiGameRoomId, int recentTurnCount, int currentTurn) {
        aiGameValidator.validateGameRoom(aiGameRoomId);

        int fromTurn = Math.max(1, currentTurn - recentTurnCount + 1);
        List<AiGameMessage> messages = aiGameMessageRepository
                .findRecentTurnsMessages(aiGameRoomId, recentTurnCount, fromTurn);

        return messages.stream()
                .map(AiGameMessageDto::fromEntity)
                .collect(Collectors.toList());
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

    /**
     * 게임방 마지막 활동 시간 업데이트 (공통 로직 추출)
     */
    private void updateRoomLastActivity(String aiGameRoomId) {
        AiGameRoom room = aiGameRoomService.getGameRoomEntity(aiGameRoomId);
        room.setLastActivity(LocalDateTime.now());
        aiGameRoomRepository.save(room);
    }

}