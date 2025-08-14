package org.com.dungeontalk.domain.chat.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.com.dungeontalk.domain.chat.common.Status;
import org.com.dungeontalk.domain.chat.entity.ChatRoomMember;
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
class ChatRoomMemberRepositoryTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    // 컨테이너 URI를 spring.data.mongodb.uri 로 주입
    @DynamicPropertySource
    static void mongoProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    // 테스트 진행 후 기록 삭제 (메모리 DB에 직전 테스트 결과 남을 경우를 고려)
    @AfterEach
    void deleteChatRoomMembers() {
        chatRoomMemberRepository.deleteAll();
    }

    private ChatRoomMember member(String roomId, String memberId, Status status) {
        Instant now = Instant.now();
        return ChatRoomMember.builder()
            .roomId(roomId)
            .memberId(memberId)
            .status(status)
            .joinedAt(now)
            .updatedAt(now)
            .build();
    }

    @Test
    @DisplayName("roomId로 전체 멤버 조회 (상태 필터링은 서비스에서)")
    void findByRoomId_returnsAllMembersOfRoom() {
        // given
        chatRoomMemberRepository.save(member("r1", "u1", Status.ONLINE));
        chatRoomMemberRepository.save(member("r1", "u2", Status.OFFLINE));
        chatRoomMemberRepository.save(member("r2", "x1", Status.ONLINE));

        // when
        List<ChatRoomMember> r1 = chatRoomMemberRepository.findByRoomId("r1");

        // then
        assertThat(r1).hasSize(2);
        assertThat(r1).extracting(ChatRoomMember::getMemberId)
            .containsExactlyInAnyOrder("u1", "u2");
    }

    @Test
    @DisplayName("roomId + memberId 로 단건 조회")
    void findByRoomIdAndMemberId() {
        // given
        chatRoomMemberRepository.save(member("r1", "u1", Status.ONLINE));

        // when / then
        assertThat(chatRoomMemberRepository.findByRoomIdAndMemberId("r1", "u1")).isPresent();
        assertThat(chatRoomMemberRepository.findByRoomIdAndMemberId("r1", "nope")).isNotPresent();
    }

    @Test
    @DisplayName("상태 업데이트 저장 동작")
    void updateStatus_persisted() {
        // given
        ChatRoomMember chatRoomMember = chatRoomMemberRepository.save(member("r1", "u1", Status.ONLINE));

        // when
        chatRoomMember.offline(Instant.now());
        chatRoomMemberRepository.save(chatRoomMember);

        // then
        ChatRoomMember found = chatRoomMemberRepository.findByRoomIdAndMemberId("r1", "u1").orElseThrow();
        assertThat(found.getStatus()).isEqualTo(Status.OFFLINE);
    }



}