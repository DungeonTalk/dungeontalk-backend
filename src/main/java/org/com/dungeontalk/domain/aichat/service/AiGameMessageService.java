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
import org.com.dungeontalk.global.filter.ProfanityFilterService;
import org.com.dungeontalk.global.filter.config.ProfanityFilterProperties;
import org.com.dungeontalk.global.filter.dto.MessageValidationResult;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
    private final ProfanityFilterService profanityFilterService;
    private final ProfanityFilterProperties profanityFilterProperties;
    private final RedisPublisher redisPublisher;
    private final ObjectMapper objectMapper;
    private final AiGameStateService aiGameStateService;
    private final AiChatErrorHandler errorHandler;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * WebSocket 메시지 처리 (컨트롤러 단순화용)
     */
    @Transactional  
    public RsData<String> handleWebSocketMessage(AiGameMessageSendRequest originalRequest) {
        AiChatLogUtils.logGameActionStart("AI 채팅 메시지 수신", originalRequest.getAiGameRoomId());

        // 욕설 필터링 처리를 lambda 밖에서 처리
        AiGameMessageSendRequest processedRequest = originalRequest;
        log.debug("AI 채팅 욕설 필터링 체크: enabled={}, filterAiChat={}", 
                 profanityFilterProperties.isEnabled(), 
                 profanityFilterProperties.isFilterAiChat());
        
        if (profanityFilterProperties.isEnabled() && profanityFilterProperties.isFilterAiChat()) {
            MessageValidationResult validation = validateMessage(originalRequest.getContent());
            
            processedRequest = handleProfanityFiltering(originalRequest, validation);
            
            if (processedRequest == null) {
                // BLOCK 모드에서 욕설이 감지되면 메시지 전송 중단
                return RsData.of("400", "부적절한 언어가 감지되어 메시지가 차단되었습니다", null);
            }
            
            if (validation.isContainsProfanity()) {
                log.info("AI 채팅 욕설 필터링 적용됨");
            }
        }

        final AiGameMessageSendRequest finalRequest = processedRequest;
        errorHandler.executeWithLogging(() -> {
            // AI 응답 처리 중인지 확인
            if (aiGameStateService.isAiProcessing(finalRequest.getAiGameRoomId())) {
                log.warn("AI 응답 처리 중이므로 메시지 전송 차단: roomId={}", finalRequest.getAiGameRoomId());
                return;
            }

            // 메시지 처리 (세션 유효성 검증은 processMessage 내부에서 처리)
            processMessage(finalRequest);

            // 세션 만료 시간 연장 시도 (세션이 유효할 때만)
            try {
                aiGameStateService.extendSession(finalRequest.getAiGameRoomId());
            } catch (Exception e) {
                log.debug("세션 연장 실패 (무시됨): roomId={}, error={}", finalRequest.getAiGameRoomId(), e.getMessage());
            }

            AiChatLogUtils.logGameAction("AI 채팅 메시지 처리", finalRequest.getAiGameRoomId(), 
                                      finalRequest.getSenderId(), finalRequest.getMessageType());
        }, "AI 채팅 메시지 처리", finalRequest.getAiGameRoomId());
        
        // AI 자동 응답 비활성화 - 수동 클릭으로만 호출
        // triggerAiResponseIfNeeded(finalRequest);
        
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
            AiGenerateRequest aiRequest = AiGenerateRequest.builder()
                    .gameId(request.getGameId())
                    .currentUser(request.getSenderId())
                    .currentMessage(request.getContent())
                    .turnNumber(request.getTurnNumber())
                    .build();
                    
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
            AiGameMessageSendRequest systemMessage = AiGameMessageSendRequest.builder()
                    .aiGameRoomId(request.getAiGameRoomId())
                    .gameId(request.getGameId())
                    .senderId(request.getSenderId())
                    .senderNickname(request.getSenderNickname())
                    .content(request.getSenderNickname() + "님이 AI 게임에 참여했습니다.")
                    .messageType(AiMessageType.SYSTEM)
                    .turnNumber(request.getTurnNumber())
                    .messageOrder(request.getMessageOrder())
                    .build();

            // 메시지 처리
            processMessage(systemMessage);

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
            AiGameMessageSendRequest systemMessage = AiGameMessageSendRequest.builder()
                    .aiGameRoomId(request.getAiGameRoomId())
                    .gameId(request.getGameId())
                    .senderId(request.getSenderId())
                    .senderNickname(request.getSenderNickname())
                    .content(request.getSenderNickname() + "님이 AI 게임에서 나갔습니다.")
                    .messageType(AiMessageType.SYSTEM)
                    .turnNumber(request.getTurnNumber())
                    .messageOrder(request.getMessageOrder())
                    .build();

            // 메시지 처리
            processMessage(systemMessage);

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
            AiGameMessageSendRequest systemMessage = AiGameMessageSendRequest.builder()
                    .aiGameRoomId(request.getAiGameRoomId())
                    .gameId(request.getGameId())
                    .senderId(SYSTEM_SENDER_ID)
                    .senderNickname(SYSTEM_SENDER_NICKNAME)
                    .content(request.getContent())
                    .messageType(AiMessageType.TURN_START)
                    .turnNumber(request.getTurnNumber())
                    .messageOrder(request.getMessageOrder())
                    .build();

            processMessage(systemMessage);

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
            AiGameMessageSendRequest systemMessage = AiGameMessageSendRequest.builder()
                    .aiGameRoomId(request.getAiGameRoomId())
                    .gameId(request.getGameId())
                    .senderId(SYSTEM_SENDER_ID)
                    .senderNickname(SYSTEM_SENDER_NICKNAME)
                    .content(request.getContent())
                    .messageType(AiMessageType.TURN_END)
                    .turnNumber(request.getTurnNumber())
                    .messageOrder(request.getMessageOrder())
                    .build();

            processMessage(systemMessage);

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
     * AI 게임 종료 처리 (컨트롤러 단순화용)
     */
    @Transactional
    public RsData<String> handleGameEnd(AiGameMessageSendRequest request) {
        try {
            // 게임 종료 메시지 생성 및 처리
            AiGameMessageSendRequest gameEndMessage = AiGameMessageSendRequest.builder()
                    .aiGameRoomId(request.getAiGameRoomId())
                    .gameId(request.getGameId())
                    .senderId(AI_SENDER_ID)
                    .senderNickname(AI_SENDER_NICKNAME)
                    .content(request.getContent())
                    .messageType(AiMessageType.GAME_END)
                    .turnNumber(request.getTurnNumber())
                    .messageOrder(request.getMessageOrder())
                    .build();

            processMessage(gameEndMessage);

            // AI 응답 처리 중 상태 해제
            aiGameStateService.unlockAfterAiResponse(request.getAiGameRoomId());

            log.info("AI 게임 종료 완료: roomId={}, result={}", 
                     request.getAiGameRoomId(), request.getContent());
            return RsData.of("200", "게임 종료 완료", null);

        } catch (AiChatException e) {
            log.error("AI 게임 종료 중 비즈니스 오류: roomId={}, errorCode={}, error={}", 
                      request.getAiGameRoomId(), e.getErrorCode().getErrorCode(), e.getMessage());
            return RsData.of("400", "게임 종료 실패: " + e.getMessage(), null);
        } catch (Exception e) {
            log.error("예상치 못한 AI 게임 종료 오류: roomId={}, error={}", 
                      request.getAiGameRoomId(), e.getMessage(), e);
            return RsData.of("500", "게임 종료 중 오류 발생", null);
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
            case GAME_END -> messageDto = handleGameEndMessage(request);
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
        try {
            aiGameValidator.validateGameRoomAndSender(request.getAiGameRoomId(), request.getSenderId());
        } catch (Exception e) {
            log.warn("게임룸/발신자 검증 실패하지만 메시지 전송 허용: roomId={}, senderId={}, error={}", 
                     request.getAiGameRoomId(), request.getSenderId(), e.getMessage());
        }

        AiGameRoom room;
        try {
            room = aiGameRoomService.getGameRoomEntity(request.getAiGameRoomId());
            
            // 턴제 검증을 완화 - TURN_INPUT이 아니어도 메시지 허용
            if (!room.getCurrentPhase().equals(AiGamePhase.TURN_INPUT)) {
                log.warn("현재 게임 페이즈가 TURN_INPUT이 아니지만 메시지 허용: roomId={}, currentPhase={}", 
                         request.getAiGameRoomId(), room.getCurrentPhase());
            }
        } catch (Exception e) {
            log.warn("게임룸 조회 실패하지만 메시지 전송 계속 진행: roomId={}, error={}", 
                     request.getAiGameRoomId(), e.getMessage());
            room = null;
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
                .createdAt(Instant.now())
                .build();

        AiGameMessage saved = aiGameMessageRepository.save(message);
        
        // 게임방 마지막 활동 시간 업데이트 (room이 null이 아닌 경우에만)
        if (room != null) {
            try {
                // lastActivity 필드 제거됨
                aiGameRoomRepository.save(room);
            } catch (Exception e) {
                log.warn("게임룸 저장 실패 (무시됨): roomId={}, error={}", request.getAiGameRoomId(), e.getMessage());
            }
        }

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
                .createdAt(Instant.now())
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
                .createdAt(Instant.now())
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
                .createdAt(Instant.now())
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
                .createdAt(Instant.now())
                .build();

        AiGameMessage saved = aiGameMessageRepository.save(message);
        log.info("턴 종료 메시지 저장 완료: roomId={}, turn={}", 
                 request.getAiGameRoomId(), request.getTurnNumber());

        return AiGameMessageDto.fromEntity(saved);
    }

    /**
     * 게임 종료 메시지 처리
     */
    @Transactional
    public AiGameMessageDto handleGameEndMessage(AiGameMessageSendRequest request) {
        aiGameValidator.validateGameRoom(request.getAiGameRoomId());

        AiGameMessage message = AiGameMessage.builder()
                .id(UuidV7Creator.create())
                .aiGameRoomId(request.getAiGameRoomId())
                .gameId(request.getGameId())
                .senderId(AI_SENDER_ID)
                .senderNickname(AI_SENDER_NICKNAME)
                .content(request.getContent())
                .messageType(AiMessageType.GAME_END)
                .turnNumber(request.getTurnNumber())
                .messageOrder(request.getMessageOrder())
                .createdAt(Instant.now())
                .build();

        AiGameMessage saved = aiGameMessageRepository.save(message);
        
        // 게임 종료 처리 - 게임 방 상태 업데이트
        try {
            AiGameRoom room = aiGameRoomService.getGameRoomEntity(request.getAiGameRoomId());
            AiGameRoom updatedRoom = room.updatePhase(AiGamePhase.GAME_END);
            aiGameRoomRepository.save(updatedRoom);
            log.info("게임 방 상태를 GAME_END로 변경: roomId={}", request.getAiGameRoomId());
        } catch (Exception e) {
            log.error("게임 방 상태 변경 실패: roomId={}, error={}", request.getAiGameRoomId(), e.getMessage());
        }

        log.info("게임 종료 메시지 저장 완료: roomId={}, result={}", 
                 request.getAiGameRoomId(), request.getContent());

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

    /**
     * 메시지 욕설 검증
     */
    private MessageValidationResult validateMessage(String message) {
        if (profanityFilterService.containsProfanity(message)) {
            String filteredMessage = profanityFilterService.filterProfanity(message);
            return MessageValidationResult.profanityDetected(message, filteredMessage);
        }
        return MessageValidationResult.success(message);
    }

    /**
     * 욕설 필터링 정책에 따른 메시지 처리
     */
    private AiGameMessageSendRequest handleProfanityFiltering(AiGameMessageSendRequest request, 
                                                            MessageValidationResult validation) {
        if (!validation.isContainsProfanity()) {
            return request; // 욕설이 없으면 그대로 반환
        }

        switch (profanityFilterProperties.getMode()) {
            case BLOCK:
                // 욕설 포함 메시지 차단 및 경고 메시지 전송
                log.warn("욕설 감지로 메시지 차단: roomId={}, userId={}, message={}", 
                        request.getAiGameRoomId(), request.getSenderId(), request.getContent());
                sendProfanityWarning(request.getAiGameRoomId(), request.getSenderId());
                return null; // null 반환으로 메시지 처리 중단

            case FILTER:
                // 욕설을 필터링하여 메시지 교체
                log.info("욕설 필터링 적용: roomId={}, userId={}, original={}, filtered={}", 
                        request.getAiGameRoomId(), request.getSenderId(), 
                        validation.getOriginalMessage(), validation.getFilteredMessage());
                
                return AiGameMessageSendRequest.builder()
                        .aiGameRoomId(request.getAiGameRoomId())
                        .gameId(request.getGameId())
                        .senderId(request.getSenderId())
                        .senderNickname(request.getSenderNickname())
                        .content(validation.getFilteredMessage()) // 필터링된 메시지로 교체
                        .messageType(request.getMessageType())
                        .turnNumber(request.getTurnNumber())
                        .messageOrder(request.getMessageOrder())
                        .build();

            case WARNING:
                // 경고 로그만 남기고 원본 메시지 통과
                log.warn("욕설 감지 (경고만 표시): roomId={}, userId={}, message={}", 
                        request.getAiGameRoomId(), request.getSenderId(), request.getContent());
                return request;

            default:
                return request;
        }
    }

    /**
     * 욕설 감지 시 경고 메시지 전송
     */
    private void sendProfanityWarning(String roomId, String userId) {
        try {
            AiGameMessageSendRequest warningMessage = AiGameMessageSendRequest.builder()
                    .aiGameRoomId(roomId)
                    .gameId("SYSTEM")
                    .senderId("SYSTEM")
                    .senderNickname("시스템")
                    .content("⚠️ 부적절한 언어가 감지되어 메시지가 차단되었습니다. 건전한 대화를 부탁드립니다.")
                    .messageType(AiMessageType.SYSTEM)
                    .turnNumber(0)
                    .messageOrder(9999)
                    .build();

            // 경고 메시지를 해당 사용자에게만 전송
            processMessage(warningMessage);
            
        } catch (Exception e) {
            log.error("욕설 경고 메시지 전송 실패: roomId={}, userId={}", roomId, userId, e);
        }
    }


}