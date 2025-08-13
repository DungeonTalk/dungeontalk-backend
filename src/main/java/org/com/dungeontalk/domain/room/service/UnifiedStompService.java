package org.com.dungeontalk.domain.room.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.room.common.RoomType;
import org.com.dungeontalk.domain.room.common.UnifiedMessageType;
import org.com.dungeontalk.domain.room.dto.UnifiedMessageRequest;
import org.springframework.stereotype.Service;

/**
 * 통합 STOMP 서비스
 * WebSocket을 통한 룸 관련 모든 비즈니스 로직을 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedStompService {

    private final RoomServiceFactory roomServiceFactory;

    /**
     * 통합된 메시지 전송 처리
     */
    public void sendMessage(String roomType, UnifiedMessageRequest request) {
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
            throw e;
        } catch (Exception e) {
            log.error("메시지 처리 중 오류 발생: roomType={}, roomId={}", 
                    roomType, request.getRoomId(), e);
            throw new RuntimeException("메시지 처리 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 통합된 룸 입장 처리
     */
    public void joinRoom(String roomType, UnifiedMessageRequest request) {
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
            throw new RuntimeException("룸 입장 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 통합된 룸 퇴장 처리
     */
    public void leaveRoom(String roomType, UnifiedMessageRequest request) {
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
            throw new RuntimeException("룸 퇴장 중 오류가 발생했습니다", e);
        }
    }

    /**
     * AI 게임 턴 시작 처리 (AI 게임 전용)
     */
    public void startTurn(UnifiedMessageRequest request) {
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
            
            log.debug("AI 게임 턴 시작 완료: roomId={}", request.getRoomId());
            
        } catch (Exception e) {
            log.error("AI 게임 턴 시작 중 오류 발생: roomId={}", request.getRoomId(), e);
            throw new RuntimeException("AI 게임 턴 시작 중 오류가 발생했습니다", e);
        }
    }

    /**
     * AI 게임 턴 종료 처리 (AI 게임 전용)
     */
    public void endTurn(UnifiedMessageRequest request) {
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
            
            log.debug("AI 게임 턴 종료 완료: roomId={}", request.getRoomId());
            
        } catch (Exception e) {
            log.error("AI 게임 턴 종료 중 오류 발생: roomId={}", request.getRoomId(), e);
            throw new RuntimeException("AI 게임 턴 종료 중 오류가 발생했습니다", e);
        }
    }
}