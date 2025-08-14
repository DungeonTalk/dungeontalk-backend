package org.com.dungeontalk.domain.chat.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.com.dungeontalk.domain.chat.common.ChatMode;
import org.com.dungeontalk.domain.chat.entity.ChatRoom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataMongoTest
@ActiveProfiles("test")
class ChatRoomRepositoryTest {

    // Mongo 6/7 이미지 어느 쪽이든 OK
    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    // 컨테이너 URI를 spring.data.mongodb.uri 로 주입
    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        // getReplicaSetUrl()은 몽고 클러스터용 연결문자열 제공
        // (버전에 따라 getConnectionString()도 사용 가능)
    }

    // 테스트 진행 후 기록 삭제 (메모리 DB에 직전 테스트 결과 남을 경우를 고려)
    @AfterEach
    void deleteChatRoom() {
        chatRoomRepository.deleteAll();
    }

    private ChatRoom newRoom(String name, ChatMode mode, Integer max) {
        Instant now = Instant.now();
        return ChatRoom.builder()
            .roomName(name)
            .mode(mode)
            .maxCapacity(max)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    @Test
    @DisplayName("채팅방 저장 후 ID로 조회된다")
    void save_and_findById() {
        // given
        ChatRoom saved = chatRoomRepository.save(newRoom("lobby", ChatMode.MULTI, 3));

        // when
        ChatRoom found = chatRoomRepository.findById(saved.getId()).orElseThrow();

        // then
        assertThat(found.getId()).isNotBlank();
        assertThat(found.getRoomName()).isEqualTo("lobby");
        assertThat(found.getMode()).isEqualTo(ChatMode.MULTI);
        assertThat(found.getMaxCapacity()).isEqualTo(3);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("여러 채팅방 저장 후 전체 조회된다")
    void findAll_returnsAll() {
        // given
        chatRoomRepository.save(newRoom("r1", ChatMode.MULTI, 3));
        chatRoomRepository.save(newRoom("r2", ChatMode.SINGLE, 1));

        // when
        List<ChatRoom> all = chatRoomRepository.findAll();

        // then
        assertThat(all).hasSize(2);
        assertThat(all).extracting(ChatRoom::getRoomName)
            .containsExactlyInAnyOrder("r1", "r2");
    }

    @Test
    @DisplayName("채팅방 업데이트가 반영된다(정원 변경 등)")
    void update_room() {
        // given
        ChatRoom room = chatRoomRepository.save(newRoom("edit-me", ChatMode.MULTI, 3));

        // when
        room.updateCapacity(10, Instant.now()); // 엔티티에 정의된 업데이트 메서드 사용
        ChatRoom updated = chatRoomRepository.save(room);

        // then
        ChatRoom found = chatRoomRepository.findById(updated.getId()).orElseThrow();
        assertThat(found.getMaxCapacity()).isEqualTo(10);
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("채팅방 삭제가 동작한다")
    void delete_room() {
        // given
        ChatRoom room = chatRoomRepository.save(newRoom("temp", ChatMode.MULTI, 2));

        // when
        chatRoomRepository.deleteById(room.getId());

        // then
        assertThat(chatRoomRepository.findById(room.getId())).isNotPresent();
    }

}