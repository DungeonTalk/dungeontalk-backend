//package org.com.dungeontalk.domain.aichat.entity;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.junit.jupiter.api.Assertions.*;
//
//import java.util.Arrays;
//import org.com.dungeontalk.domain.aichat.common.AiGameStatus;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//class AiGameRoomTest {
//
//    @Test
//    @DisplayName("게임방이 ACTIVE 상태일 때 isActive()는 true를 반환한다.")
//    void isActive_true_whenStatusActive() {
//        AiGameRoom room = AiGameRoom.builder()
//            .status(AiGameStatus.ACTIVE)
//            .maxParticipants(3)
//            .build();
//
//        assertThat(room.isActive()).isTrue();
//    }
//
//    @Test
//    @DisplayName("게임방이 CREATED 상태이고 정원이 안 찼으면 canJoin()은 true를 반환한다.")
//    void canJoin_true_whenCreatedAndNotFull() {
//        AiGameRoom room = AiGameRoom.builder()
//            .status(AiGameStatus.CREATED)
//            .maxParticipants(3)
//            .participants(Arrays.asList("m1", "m2"))
//            .build();
//
//        assertThat(room.canJoin()).isTrue();
//    }
//
//    @Test
//    @DisplayName("게임방이 CREATED 상태지만 정원이 가득 차면 canJoin()은 false")
//    void canJoin_false_whenFull() {
//        AiGameRoom room = AiGameRoom.builder()
//            .status(AiGameStatus.CREATED)
//            .maxParticipants(2)
//            .participants(Arrays.asList("m1", "m2"))
//            .build();
//
//        assertThat(room.canJoin()).isFalse();
//    }
//
//    @Test
//    @DisplayName("게임방 참여자 리스트가 null인 경우 getCurrentParticipantCount()는 0")
//    void getCurrentParticipantCount_returnsZeroIfNull() {
//        AiGameRoom room = AiGameRoom.builder()
//            .status(AiGameStatus.CREATED)
//            .maxParticipants(5)
//            .participants(null)
//            .build();
//
//        assertThat(room.getCurrentParticipantCount()).isZero();
//    }
//
//    @Test
//    @DisplayName("게임방 참여자 수를 정확하게 반환한다.")
//    void getCurrentParticipantCount_returnsSize() {
//        AiGameRoom room = AiGameRoom.builder()
//            .status(AiGameStatus.CREATED)
//            .maxParticipants(5)
//            .participants(Arrays.asList("m1", "m2", "m3"))
//            .build();
//
//        assertThat(room.getCurrentParticipantCount()).isEqualTo(3);
//    }
//
//}