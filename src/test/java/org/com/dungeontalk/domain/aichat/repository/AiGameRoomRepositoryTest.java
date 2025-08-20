package org.com.dungeontalk.domain.aichat.repository;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.com.dungeontalk.domain.aichat.common.AiGameStatus;
import org.com.dungeontalk.domain.aichat.entity.AiGameRoom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiGameRoomRepositoryTest {

    @Mock
    private AiGameRoomRepository aiGameRoomRepository;

    @Test
    @DisplayName("findByGameId()로 방을 찾으면 Optional 값이 반환된다")
    void findByGameId_returnsOptional() {
        AiGameRoom dummy = AiGameRoom.builder()
            .id("id-1")
            .gameId("game-123")
            .status(AiGameStatus.CREATED)
            .maxParticipants(4)
            .build();

        when(aiGameRoomRepository.findByGameId("game-123")).thenReturn(Optional.of(dummy));

        Optional<AiGameRoom> result = aiGameRoomRepository.findByGameId("game-123");

        assertThat(result).isPresent();
        assertThat(result.get().getGameId()).isEqualTo("game-123");
    }

    @Test
    @DisplayName("findByParticipantsContaining()으로 특정 멤버가 참여한 방 목록 조회")
    void findByParticipantsContaining_returnsList() {
        AiGameRoom room = AiGameRoom.builder()
            .id("room-1")
            .gameId("game-xyz")
            .participants(List.of("member-1", "member-2"))
            .status(AiGameStatus.CREATED)
            .maxParticipants(3)
            .build();

        when(aiGameRoomRepository.findByParticipantsContaining("member-1"))
            .thenReturn(List.of(room));

        List<AiGameRoom> result = aiGameRoomRepository.findByParticipantsContaining("member-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getParticipants()).contains("member-1");
    }

}