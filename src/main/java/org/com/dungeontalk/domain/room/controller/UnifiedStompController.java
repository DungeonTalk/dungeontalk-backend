package org.com.dungeontalk.domain.room.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.room.dto.UnifiedMessageRequest;
import org.com.dungeontalk.domain.room.service.RoomService;
import org.com.dungeontalk.domain.room.service.RoomServiceFactory;
import org.com.dungeontalk.domain.room.common.RoomType;
import org.com.dungeontalk.domain.room.common.UnifiedMessageType;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.stereotype.Controller;

import jakarta.validation.Valid;

/**
 * 통합된 STOMP 컨트롤러
 * AI 채팅과 플레이어 채팅의 WebSocket 메시지를 통합 처리
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class UnifiedStompController {

    private final RoomServiceFactory roomServiceFactory;

    /**
     * 통합된 메시지 전송 엔드포인트
     * 
     * 클라이언트는 /pub/room/{roomType}/send 로 메시지를 발행
     * roomType: "ai" (AI 게임) 또는 "chat" (플레이어 채팅)
     * 
     * 예시:
     * - AI 게임 메시지: /pub/room/ai/send
     * - 플레이어 채팅: /pub/room/chat/send
     */
    @MessageMapping("/room/{roomType}/send")
    public void sendMessage(@DestinationVariable String roomType, 
                          @Valid @Payload UnifiedMessageRequest request) {
        log.debug("통합 메시지 수신: roomType={}, roomId={}, senderId={}, messageType={}", 
                roomType, request.getRoomId(), request.getSenderId(), request.getMessageType());
        
        try {
            // 요청 유효성 검증
            request.validateByRoomType();
            
            // roomType과 요청의 roomType 일치 확인 및 빌더로 새 객체 생성
            if (!roomType.equals(request.getRoomType().getCode())) {
                log.warn("경로의 roomType({})과 요청의 roomType({})이 일치하지 않음", 
                        roomType, request.getRoomType().getCode());
                request = request.toBuilder()
                        .roomType(RoomType.fromCode(roomType))
                        .build();
            }
            
            // 해당 타입의 서비스 가져오기
            RoomService roomService = roomServiceFactory.getService(roomType);
            
            // 메시지 처리
            roomService.processMessage(request);
            
            log.debug("메시지 처리 완료: roomType={}, roomId={}, messageType={}", 
                    roomType, request.getRoomId(), request.getMessageType());
            
        } catch (IllegalArgumentException e) {
            log.warn("메시지 처리 실패 - 잘못된 요청: roomType={}, error={}", roomType, e.getMessage());
            // TODO: 클라이언트에게 에러 메시지 전송
        } catch (Exception e) {
            log.error("메시지 처리 중 오류 발생: roomType={}, roomId={}", 
                    roomType, request.getRoomId(), e);
            // TODO: 클라이언트에게 에러 메시지 전송
        }
    }

    /**
     * 통합된 룸 입장 엔드포인트
     * 
     * 클라이언트는 /pub/room/{roomType}/join 으로 입장 요청을 발행
     */
    @MessageMapping("/room/{roomType}/join")
    public void joinRoom(@DestinationVariable String roomType,
                        @Valid @Payload UnifiedMessageRequest request) {
        log.info("룸 입장 요청: roomType={}, roomId={}, senderId={}", 
                roomType, request.getRoomId(), request.getSenderId());
        
        try {
            // roomType 설정 및 빌더로 새 객체 생성
            request = request.toBuilder()
                    .roomType(RoomType.fromCode(roomType))
                    .messageType(UnifiedMessageType.SYSTEM)
                    .content(request.getSenderId() + "님이 입장했습니다.")
                    .build();
            
            // 요청 유효성 검증
            request.validateByRoomType();
            
            // 해당 타입의 서비스 가져오기
            RoomService roomService = roomServiceFactory.getService(roomType);
            
            // 룸 입장 처리
            roomService.joinRoom(request.getRoomId(), request.getSenderId());
            
            // 입장 메시지 전송
            roomService.processMessage(request);
            
            log.info("룸 입장 완료: roomType={}, roomId={}, senderId={}", 
                    roomType, request.getRoomId(), request.getSenderId());
            
        } catch (Exception e) {
            log.error("룸 입장 중 오류 발생: roomType={}, roomId={}, senderId={}", 
                    roomType, request.getRoomId(), request.getSenderId(), e);
        }
    }

    /**
     * 통합된 룸 퇴장 엔드포인트
     * 
     * 클라이언트는 /pub/room/{roomType}/leave 로 퇴장 요청을 발행
     */
    @MessageMapping("/room/{roomType}/leave")
    public void leaveRoom(@DestinationVariable String roomType,
                         @Valid @Payload UnifiedMessageRequest request) {
        log.info("룸 퇴장 요청: roomType={}, roomId={}, senderId={}", 
                roomType, request.getRoomId(), request.getSenderId());
        
        try {
            // roomType 설정 및 빌더로 새 객체 생성
            request = request.toBuilder()
                    .roomType(RoomType.fromCode(roomType))
                    .messageType(UnifiedMessageType.SYSTEM)
                    .content(request.getSenderId() + "님이 퇴장했습니다.")
                    .build();
            
            // 요청 유효성 검증
            request.validateByRoomType();
            
            // 해당 타입의 서비스 가져오기
            RoomService roomService = roomServiceFactory.getService(roomType);
            
            // 퇴장 메시지 전송 (퇴장 전에)
            roomService.processMessage(request);
            
            // 룸 퇴장 처리
            roomService.leaveRoom(request.getRoomId(), request.getSenderId());
            
            log.info("룸 퇴장 완료: roomType={}, roomId={}, senderId={}", 
                    roomType, request.getRoomId(), request.getSenderId());
            
        } catch (Exception e) {
            log.error("룸 퇴장 중 오류 발생: roomType={}, roomId={}, senderId={}", 
                    roomType, request.getRoomId(), request.getSenderId(), e);
        }
    }

    // === AI 게임 전용 엔드포인트 (하위 호환성) ===

    /**
     * AI 게임 턴 시작 (AI 게임 전용)
     */
    @MessageMapping("/room/ai/turn/start")
    public void startTurn(@Valid @Payload UnifiedMessageRequest request) {
        log.debug("AI 게임 턴 시작: roomId={}", request.getRoomId());
        
        try {
            request = request.toBuilder()
                    .roomType(RoomType.AI_GAME)
                    .messageType(UnifiedMessageType.GAME_STATE)
                    .content(request.getContent() != null && !request.getContent().trim().isEmpty() 
                            ? request.getContent() : "새로운 턴이 시작되었습니다.")
                    .build();
            
            RoomService roomService = roomServiceFactory.getService("ai");
            roomService.processMessage(request);
            
        } catch (Exception e) {
            log.error("AI 게임 턴 시작 중 오류 발생: roomId={}", request.getRoomId(), e);
        }
    }

    /**
     * AI 게임 턴 종료 (AI 게임 전용)
     */
    @MessageMapping("/room/ai/turn/end")
    public void endTurn(@Valid @Payload UnifiedMessageRequest request) {
        log.debug("AI 게임 턴 종료: roomId={}", request.getRoomId());
        
        try {
            request = request.toBuilder()
                    .roomType(RoomType.AI_GAME)
                    .messageType(UnifiedMessageType.GAME_STATE)
                    .content(request.getContent() != null && !request.getContent().trim().isEmpty() 
                            ? request.getContent() : "턴이 종료되었습니다.")
                    .build();
            
            RoomService roomService = roomServiceFactory.getService("ai");
            roomService.processMessage(request);
            
        } catch (Exception e) {
            log.error("AI 게임 턴 종료 중 오류 발생: roomId={}", request.getRoomId(), e);
        }
    }
}