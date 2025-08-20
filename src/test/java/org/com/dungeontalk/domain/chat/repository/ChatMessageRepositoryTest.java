package org.com.dungeontalk.domain.chat.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.com.dungeontalk.domain.chat.common.MessageType;
import org.com.dungeontalk.domain.chat.entity.ChatMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataMongoTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    // test 프로필의 exclude 무력화
    "spring.autoconfigure.exclude="
})
class ChatMessageRepositoryTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    // 컨테이너 URI를 spring.data.mongodb.uri 로 주입
    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    // 테스트 진행 후 기록 삭제 (메모리 DB에 직전 테스트 결과 남을 경우를 고려)
    @AfterEach
    void deleteChatMessages() {
        chatMessageRepository.deleteAll();
    }

    private ChatMessage newMsg(String roomId, String senderId, String content, Instant when) {
        return ChatMessage.builder()
            .messageId(UUID.randomUUID().toString())
            .roomId(roomId)
            .senderId(senderId)
            .content(content)
            .type(MessageType.TALK)
            .createdAt(when)
            .updatedAt(when)
            .build();
    }

    @Test
    @DisplayName("roomId 기준 최신순 페이징 조회")
    void findByRoomId_paging_desc() {
        // given
        String room1 = "room-1";
        chatMessageRepository.save(newMsg(room1, "u1", "m1", Instant.now().minusSeconds(30)));
        chatMessageRepository.save(newMsg(room1, "u2", "m2", Instant.now().minusSeconds(20)));
        chatMessageRepository.save(newMsg(room1, "u3", "m3", Instant.now().minusSeconds(10)));
        chatMessageRepository.save(newMsg("room-2", "x1", "other", Instant.now()));

        // when
        PageRequest page0 = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ChatMessage> p0 = chatMessageRepository.findByRoomId(room1, page0);

        // then
        assertThat(p0.getTotalElements()).isEqualTo(3);
        assertThat(p0.getContent()).extracting(ChatMessage::getContent)
            .containsExactly("m3", "m2");

        // when 2
        PageRequest page1 = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ChatMessage> p1 = chatMessageRepository.findByRoomId(room1, page1);

        // then 2
        assertThat(p1.getContent()).extracting(ChatMessage::getContent)
            .containsExactly("m1");
    }

    @Test
    @DisplayName("메시지 저장 및 단건 조회")
    void save_and_find() {
        // given
        ChatMessage saved = chatMessageRepository.save(
            newMsg("room-9", "u9", "hello", Instant.now())
        );

        // when / then
        assertThat(chatMessageRepository.findById(saved.getMessageId())).isPresent();
        assertThat(saved.getMessageId()).isNotBlank();
    }

}