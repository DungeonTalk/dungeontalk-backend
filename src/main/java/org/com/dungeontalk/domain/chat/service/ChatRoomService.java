package org.com.dungeontalk.domain.chat.service;

import static org.com.dungeontalk.domain.chat.dto.ChatRoomDto.fromEntity;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.chat.dto.ChatRoomDto;
import org.com.dungeontalk.domain.chat.dto.request.ChatRoomCreateRequestDto;
import org.com.dungeontalk.domain.chat.entity.ChatRoom;
import org.com.dungeontalk.domain.chat.repository.ChatRoomRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;

    // 채팅방 생성
    public ChatRoomDto createRoom(ChatRoomCreateRequestDto req) {
        ChatRoom chatRoom = ChatRoom.builder()
            .roomName(req.getRoomName())
            .roomType(req.getRoomType())
            .mode(req.getMode())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        ChatRoom saved = chatRoomRepository.save(chatRoom);
        return fromEntity(saved);
    }

    // 채팅방 단일 조회
    public ChatRoomDto getRoomById(String roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + roomId));

        return fromEntity(room);
    }

    // 채팅방 전체 조회
    public List<ChatRoomDto> getAllRooms() {
        return chatRoomRepository.findAll().stream()
            .map(ChatRoomDto::fromEntity)
            .toList();
    }

    // 참여자 입장
    public void joinRoom(String roomId, String memberId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("채팅방 없음"));

        if (!room.getParticipants().contains(memberId)) {
            room.getParticipants().add(memberId);
            chatRoomRepository.save(room);
        }
    }

    // 참여자 퇴장
    public void leaveRoom(String roomId, String memberId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("채팅방 없음"));

        room.getParticipants().remove(memberId);
        chatRoomRepository.save(room);
    }


}
