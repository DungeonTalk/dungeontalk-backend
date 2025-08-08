package org.com.dungeontalk.domain.aichat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.aichat.common.AiGamePhase;
import org.com.dungeontalk.domain.aichat.common.AiGameStatus;
import org.com.dungeontalk.domain.aichat.dto.AiGameRoomDto;
import org.com.dungeontalk.domain.aichat.dto.request.AiGameRoomCreateRequest;
import org.com.dungeontalk.domain.aichat.dto.request.AiGameRoomJoinRequest;
import org.com.dungeontalk.domain.aichat.dto.response.AiGameRoomResponse;
import org.com.dungeontalk.domain.aichat.entity.AiGameRoom;
import org.com.dungeontalk.domain.aichat.repository.AiGameRoomRepository;
import org.com.dungeontalk.domain.member.entity.Member;
import org.com.dungeontalk.domain.member.repository.MemberRepository;
import org.com.dungeontalk.global.util.UuidV7Creator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiGameRoomService {

    private final AiGameRoomRepository aiGameRoomRepository;
    private final MemberRepository memberRepository;

    /**
     * AI 게임방 생성
     */
    @Transactional
    public AiGameRoomResponse createAiGameRoom(AiGameRoomCreateRequest request) {
        validateCreator(request.getCreatorId());

        AiGameRoom aiGameRoom = AiGameRoom.builder()
                .id(UuidV7Creator.create())
                .gameId(request.getGameId())
                .roomName(request.getRoomName())
                .description(request.getDescription())
                .status(AiGameStatus.CREATED)
                .currentPhase(AiGamePhase.WAITING)
                .currentTurn(1)
                .maxParticipants(request.getMaxParticipants())
                .participants(new ArrayList<>())
                .gameSettings(request.getGameSettings())
                .lastActivity(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 생성자를 첫 번째 참여자로 추가
        aiGameRoom.getParticipants().add(request.getCreatorId());

        AiGameRoom saved = aiGameRoomRepository.save(aiGameRoom);
        log.info("AI 게임방 생성 완료: roomId={}, gameId={}, creator={}", 
                 saved.getId(), saved.getGameId(), request.getCreatorId());

        return convertToResponse(saved);
    }

    /**
     * AI 게임방 참여
     */
    @Transactional
    public AiGameRoomResponse joinAiGameRoom(AiGameRoomJoinRequest request) {
        validateParticipant(request.getParticipantId());

        AiGameRoom room = aiGameRoomRepository.findById(request.getAiGameRoomId())
                .orElseThrow(() -> new IllegalArgumentException("AI 게임방을 찾을 수 없습니다: " + request.getAiGameRoomId()));

        if (!room.canJoin()) {
            throw new IllegalStateException("입장할 수 없는 게임방입니다. 상태: " + room.getStatus() + 
                                          ", 참여자 수: " + room.getCurrentParticipantCount() + "/" + room.getMaxParticipants());
        }

        if (room.getParticipants().contains(request.getParticipantId())) {
            throw new IllegalStateException("이미 참여중인 게임방입니다.");
        }

        // 참여자 추가
        room.getParticipants().add(request.getParticipantId());
        room.setLastActivity(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());

        // 정원이 찼으면 게임 시작
        if (room.getCurrentParticipantCount() >= room.getMaxParticipants()) {
            room.setStatus(AiGameStatus.ACTIVE);
            room.setCurrentPhase(AiGamePhase.TURN_INPUT);
        }

        AiGameRoom saved = aiGameRoomRepository.save(room);
        log.info("AI 게임방 참여 완료: roomId={}, participant={}, currentCount={}", 
                 saved.getId(), request.getParticipantId(), saved.getCurrentParticipantCount());

        return convertToResponse(saved);
    }

    /**
     * AI 게임방 퇴장
     */
    @Transactional
    public void leaveAiGameRoom(String aiGameRoomId, String participantId) {
        AiGameRoom room = aiGameRoomRepository.findById(aiGameRoomId)
                .orElseThrow(() -> new IllegalArgumentException("AI 게임방을 찾을 수 없습니다: " + aiGameRoomId));

        if (!room.getParticipants().contains(participantId)) {
            throw new IllegalStateException("참여하지 않은 게임방입니다.");
        }

        room.getParticipants().remove(participantId);
        room.setLastActivity(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());

        // 참여자가 모두 나가면 게임 종료
        if (room.getParticipants().isEmpty()) {
            room.setStatus(AiGameStatus.COMPLETED);
            room.setCurrentPhase(AiGamePhase.GAME_END);
        }

        aiGameRoomRepository.save(room);
        log.info("AI 게임방 퇴장 완료: roomId={}, participant={}, remainingCount={}", 
                 room.getId(), participantId, room.getCurrentParticipantCount());
    }

    /**
     * 특정 AI 게임방 조회
     */
    public AiGameRoomResponse getAiGameRoom(String aiGameRoomId) {
        AiGameRoom room = aiGameRoomRepository.findById(aiGameRoomId)
                .orElseThrow(() -> new IllegalArgumentException("AI 게임방을 찾을 수 없습니다: " + aiGameRoomId));
        
        return convertToResponse(room);
    }

    /**
     * 게임 ID로 AI 게임방 조회
     */
    public AiGameRoomResponse getAiGameRoomByGameId(String gameId) {
        AiGameRoom room = aiGameRoomRepository.findByGameId(gameId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게임의 AI 게임방을 찾을 수 없습니다: " + gameId));
        
        return convertToResponse(room);
    }

    /**
     * 입장 가능한 AI 게임방 목록 조회
     */
    public List<AiGameRoomResponse> getAvailableRooms() {
        List<AiGameRoom> rooms = aiGameRoomRepository.findAvailableRooms();
        return rooms.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 특정 사용자가 참여중인 AI 게임방 목록 조회
     */
    public List<AiGameRoomResponse> getUserParticipatingRooms(String participantId) {
        List<AiGameRoom> rooms = aiGameRoomRepository.findByParticipantsContaining(participantId);
        return rooms.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * AI 게임방 상태 업데이트
     */
    @Transactional
    public AiGameRoomResponse updateGamePhase(String aiGameRoomId, AiGamePhase newPhase) {
        AiGameRoom room = aiGameRoomRepository.findById(aiGameRoomId)
                .orElseThrow(() -> new IllegalArgumentException("AI 게임방을 찾을 수 없습니다: " + aiGameRoomId));

        room.setCurrentPhase(newPhase);
        room.setLastActivity(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());

        AiGameRoom saved = aiGameRoomRepository.save(room);
        log.info("AI 게임방 페이즈 업데이트: roomId={}, newPhase={}", aiGameRoomId, newPhase);

        return convertToResponse(saved);
    }

    /**
     * AI 게임방 턴 증가
     */
    @Transactional
    public AiGameRoomResponse nextTurn(String aiGameRoomId) {
        AiGameRoom room = aiGameRoomRepository.findById(aiGameRoomId)
                .orElseThrow(() -> new IllegalArgumentException("AI 게임방을 찾을 수 없습니다: " + aiGameRoomId));

        room.setCurrentTurn(room.getCurrentTurn() + 1);
        room.setCurrentPhase(AiGamePhase.TURN_INPUT);
        room.setLastActivity(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());

        AiGameRoom saved = aiGameRoomRepository.save(room);
        log.info("AI 게임방 턴 증가: roomId={}, newTurn={}", aiGameRoomId, saved.getCurrentTurn());

        return convertToResponse(saved);
    }

    private void validateCreator(String creatorId) {
        memberRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("생성자 정보를 찾을 수 없습니다: " + creatorId));
    }

    private void validateParticipant(String participantId) {
        memberRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("참여자 정보를 찾을 수 없습니다: " + participantId));
    }

    private AiGameRoomResponse convertToResponse(AiGameRoom room) {
        return AiGameRoomResponse.builder()
                .id(room.getId())
                .gameId(room.getGameId())
                .roomName(room.getRoomName())
                .description(room.getDescription())
                .status(room.getStatus())
                .currentPhase(room.getCurrentPhase())
                .currentTurn(room.getCurrentTurn())
                .maxParticipants(room.getMaxParticipants())
                .currentParticipantCount(room.getCurrentParticipantCount())
                .participants(new ArrayList<>(room.getParticipants()))
                .lastActivity(room.getLastActivity())
                .createdAt(room.getCreatedAt())
                .build();
    }
}