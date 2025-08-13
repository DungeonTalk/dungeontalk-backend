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
import org.com.dungeontalk.domain.aichat.util.AiChatErrorHandler;
import org.com.dungeontalk.domain.aichat.util.AiChatLogUtils;
import org.com.dungeontalk.domain.aichat.util.AiGameValidator;
import org.com.dungeontalk.domain.aichat.service.AiGameRoomService;
import org.com.dungeontalk.domain.aichat.event.AiTurnProcessEvent;
import org.com.dungeontalk.domain.aichat.dto.request.AiGenerateRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.com.dungeontalk.global.exception.ErrorCode;
import org.com.dungeontalk.global.exception.customException.AiChatException;
import org.com.dungeontalk.global.rsData.RsData;
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
    private final AiGameStateService aiGameStateService;
    private final AiChatErrorHandler errorHandler;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * WebSocket 메시지 처리 (컨트롤러 단순화용)
     */
    @Transactional  
    public RsData<String> handleWebSocketMessage(AiGameMessageSendRequest request) {
        AiChatLogUtils.logGameActionStart("AI 채팅 메시지 수신", request.getAiGameRoomId());

        errorHandler.executeWithLogging(() -> {
            // AI 응답 처리 중인지 확인
            if (aiGameStateService.isAiProcessing(request.getAiGameRoomId())) {
                log.warn("AI 응답 처리 중이므로 메시지 전송 차단: roomId={}", request.getAiGameRoomId());
                return;
            }

            // 세션 유효성 검증
            if (!aiGameStateService.isSessionValid(request.getAiGameRoomId())) {
                log.warn("유효하지 않은 게임 세션: roomId={}", request.getAiGameRoomId());
                return;
            }

            // 메시지 처리
            processMessage(request);

            // 세션 만료 시간 연장
            aiGameStateService.extendSession(request.getAiGameRoomId());

            AiChatLogUtils.logGameAction("AI 채팅 메시지 처리", request.getAiGameRoomId(), 
                                      request.getSenderId(), request.getMessageType());
        }, "AI 채팅 메시지 처리", request.getAiGameRoomId());
        
        // 사용자 메시지인 경우 AI 응답 자동 트리거
        triggerAiResponseIfNeeded(request);
        
        return RsData.of("200", "메시지 전송 완료", null);
    }
    
    /**
     * 필요시 AI 응답 트리거 (비즈니스 로직)
     */
    private void triggerAiResponseIfNeeded(AiGameMessageSendRequest request) {
        if (request.getMessageType() != AiMessageType.USER) {
            return; // USER 타입이 아니면 AI 트리거 안 함
        }
        
        try {
            AiGenerateRequest aiRequest = new AiGenerateRequest();
            aiRequest.setGameId(request.getGameId());
            aiRequest.setCurrentUser(request.getSenderId());
            aiRequest.setCurrentMessage(request.getContent());
            aiRequest.setTurnNumber(request.getTurnNumber());
                    
            log.info("AI 응답 자동 트리거: roomId={}, user={}, turn={}", 
                     request.getAiGameRoomId(), request.getSenderId(), request.getTurnNumber());
                     
            eventPublisher.publishEvent(new AiTurnProcessEvent(request.getAiGameRoomId(), aiRequest));
            
        } catch (Exception e) {
            log.error("AI 응답 자동 트리거 실패: roomId={}, error={}", 
                     request.getAiGameRoomId(), e.getMessage(), e);
            // 예외를 다시 던지지 않음 - 메시지 전송은 성공했으므로
        }
    }

    /**
     * AI 채팅방 입장 처리 (컨트롤러 단순화용)
     */
    @Transactional
    public RsData<String> handleJoinRoom(AiGameMessageSendRequest request) {
        try {
            // 입장 시스템 메시지 생성
            request.setContent(request.getSenderNickname() + "님이 AI 게임에 참여했습니다.");
            request.setMessageType(AiMessageType.SYSTEM);

            // 메시지 처리
            processMessage(request);

            log.info("AI 채팅방 입장 완료: roomId={}, participant={}", 
                     request.getAiGameRoomId(), request.getSenderId());
            return RsData.of("200", "게임방 입장 완료", null);

        } catch (AiChatException e) {
            log.error("AI 채팅방 입장 중 비즈니스 오류: roomId={}, errorCode={}, error={}", 
                      request.getAiGameRoomId(), e.getErrorCode().getErrorCode(), e.getMessage());
            return RsData.of("400", "게임방 입장 실패: " + e.getMessage(), null);
        } catch (Exception e) {
            log.error("예상치 못한 AI 채팅방 입장 오류: roomId={}, error={}", 
                      request.getAiGameRoomId(), e.getMessage(), e);
            return RsData.of("500", "게임방 입장 중 오류 발생", null);
        }
    }

    /**
     * AI 채팅방 퇴장 처리 (컨트롤러 단순화용)
     */
    @Transactional
    public RsData<String> handleLeaveRoom(AiGameMessageSendRequest request) {
        try {
            // 퇴장 시스템 메시지 생성
            request.setContent(request.getSenderNickname() + "님이 AI 게임에서 나갔습니다.");
            request.setMessageType(AiMessageType.SYSTEM);

            // 메시지 처리
            processMessage(request);

            log.info("AI 채팅방 퇴장 완료: roomId={}, participant={}", 
                     request.getAiGameRoomId(), request.getSenderId());
            return RsData.of("200", "게임방 퇴장 완료", null);

        } catch (AiChatException e) {
            log.error("AI 채팅방 퇴장 중 비즈니스 오류: roomId={}, errorCode={}, error={}", 
                      request.getAiGameRoomId(), e.getErrorCode().getErrorCode(), e.getMessage());
            return RsData.of("400", "게임방 퇴장 실패: " + e.getMessage(), null);
        } catch (Exception e) {
            log.error("예상치 못한 AI 채팅방 퇴장 오류: roomId={}, error={}", 
                      request.getAiGameRoomId(), e.getMessage(), e);
            return RsData.of("500", "게임방 퇴장 중 오류 발생", null);
        }
    }

    /**
     * AI 게임 턴 시작 처리 (컨트롤러 단순화용)
     */
    @Transactional
    public RsData<String> handleStartTurn(AiGameMessageSendRequest request) {
        try {
            request.setMessageType(AiMessageType.TURN_START);
            request.setSenderId(SYSTEM_SENDER_ID);
            request.setSenderNickname(SYSTEM_SENDER_NICKNAME);

            processMessage(request);

            log.info("AI 게임 턴 시작: roomId={}, turn={}", 
                     request.getAiGameRoomId(), request.getTurnNumber());
            return RsData.of("200", "턴 시작 완료", null);

        } catch (AiChatException e) {
            log.error("AI 게임 턴 시작 중 비즈니스 오류: roomId={}, errorCode={}, error={}", 
                      request.getAiGameRoomId(), e.getErrorCode().getErrorCode(), e.getMessage());
            return RsData.of("400", "턴 시작 실패: " + e.getMessage(), null);
        } catch (Exception e) {
            log.error("예상치 못한 AI 게임 턴 시작 오류: roomId={}, error={}", 
                      request.getAiGameRoomId(), e.getMessage(), e);
            return RsData.of("500", "턴 시작 중 오류 발생", null);
        }
    }

    /**
     * AI 게임 턴 종료 처리 (컨트롤러 단순화용)
     */
    @Transactional
    public RsData<String> handleEndTurn(AiGameMessageSendRequest request) {
        try {
            request.setMessageType(AiMessageType.TURN_END);
            request.setSenderId(SYSTEM_SENDER_ID);
            request.setSenderNickname(SYSTEM_SENDER_NICKNAME);

            processMessage(request);

            // AI 응답 완료 후 락 해제
            aiGameStateService.unlockAfterAiResponse(request.getAiGameRoomId());

            log.info("AI 게임 턴 종료: roomId={}, turn={}", 
                     request.getAiGameRoomId(), request.getTurnNumber());
            return RsData.of("200", "턴 종료 완료", null);

        } catch (AiChatException e) {
            log.error("AI 게임 턴 종료 중 비즈니스 오류: roomId={}, errorCode={}, error={}", 
                      request.getAiGameRoomId(), e.getErrorCode().getErrorCode(), e.getMessage());
            return RsData.of("400", "턴 종료 실패: " + e.getMessage(), null);
        } catch (Exception e) {
            log.error("예상치 못한 AI 게임 턴 종료 오류: roomId={}, error={}", 
                      request.getAiGameRoomId(), e.getMessage(), e);
            return RsData.of("500", "턴 종료 중 오류 발생", null);
        }
    }

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

        // 직접 WebSocket 브로드캐스트 제거 - Redis pub/sub를 통해서만 브로드캐스트
        // String destination = WEBSOCKET_DESTINATION_PREFIX + request.getAiGameRoomId();
        // messagingTemplate.convertAndSend(destination, messageDto);

        // AI 채팅 전용 Redis 메시지 브로드캐스트 (단일 브로드캐스트)
        String json = objectMapper.writeValueAsString(messageDto);
        redisPublisher.publishAiChat(request.getAiGameRoomId(), json);

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
        // lastActivity 필드 제거됨
        aiGameRoomRepository.save(room);

        log.info("사용자 메시지 저장 완료: roomId={}, sender={}, turn={}", 
                 request.getAiGameRoomId(), request.getSenderId(), request.getTurnNumber());

        // TODO: AI 응답 트리거는 별도 컴포넌트에서 처리
        // (순환 의존성 방지를 위해 직접 호출하지 않음)

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
                .createdAt(LocalDateTime.now())
                .build();

        AiGameMessage saved = aiGameMessageRepository.save(message);

        // 게임방 마지막 활동 시간 업데이트
        updateRoomLastActivity(request.getAiGameRoomId());

        // AI 메시지를 WebSocket으로 브로드캐스트
        AiGameMessageDto messageDto = AiGameMessageDto.fromEntity(saved);
        try {
            String json = objectMapper.writeValueAsString(messageDto);
            redisPublisher.publishAiChat(request.getAiGameRoomId(), json);
            log.info("AI 메시지 WebSocket 브로드캐스트 완료: roomId={}", request.getAiGameRoomId());
        } catch (Exception e) {
            log.error("AI 메시지 브로드캐스트 실패: roomId={}, error={}", request.getAiGameRoomId(), e.getMessage());
        }

        log.info("AI 메시지 저장 완료: roomId={}, turn={}, responseTime={}ms", 
                 request.getAiGameRoomId(), request.getTurnNumber());

        return messageDto;
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
        // lastActivity 필드 제거됨
        aiGameRoomRepository.save(room);
    }

    /**
     * AI 턴 처리 (Controller에서 호출)
     */
    public void processAiTurn(String aiGameRoomId, AiGenerateRequest aiRequest) {
        eventPublisher.publishEvent(new AiTurnProcessEvent(aiGameRoomId, aiRequest));
    }


}