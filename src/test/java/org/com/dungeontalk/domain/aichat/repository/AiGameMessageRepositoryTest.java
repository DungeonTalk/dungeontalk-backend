//package org.com.dungeontalk.domain.aichat.repository;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//import java.time.Instant;
//import java.util.List;
//import java.util.UUID;
//
//import org.com.dungeontalk.domain.aichat.entity.AiGameMessage;
//import org.com.dungeontalk.domain.aichat.common.AiMessageType;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.testcontainers.containers.MongoDBContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//@Testcontainers        // Docker 컨테이너 자동 관리
//@DataMongoTest         // MongoDB Repository 테스트 전용
//@ActiveProfiles("test") // 테스트 설정 사용
//class AiGameMessageRepositoryTest {
//
//    // MongoDB 컨테이너 설정 (Chat과 동일)
//    @Container
//    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");
//
//    // 컨테이너 URI를 Spring에 주입
//    @DynamicPropertySource
//    static void mongoProps(DynamicPropertyRegistry registry) {
//        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
//    }
//
//    @Autowired
//    private AiGameMessageRepository repository;
//
//    // 테스트 후 데이터 정리 (다른 테스트에 영향 안 주려고)
//    @AfterEach
//    void cleanup() {
//        repository.deleteAll();
//    }
//
//    // 테스트용 메시지 생성 헬퍼 메서드
//    private AiGameMessage createTestMessage(String roomId, String content, int turnNumber, Instant when) {
//        return AiGameMessage.builder()
//                .id(UUID.randomUUID().toString())
//                .aiGameRoomId(roomId)
//                .content(content)
//                .messageType(AiMessageType.USER)
//                .senderNickname("테스터")
//                .turnNumber(turnNumber)
//                .messageOrder(1)
//                .createdAt(when)
//                .build();
//    }
//
//    @Test
//    @DisplayName("메시지 저장 및 조회 기본 테스트")
//    void saveAndFind() {
//        // given
//        AiGameMessage message = createTestMessage("room-1", "안녕하세요", 1, Instant.now());
//
//        // when
//        AiGameMessage saved = repository.save(message);
//
//        // then
//        assertThat(saved.getId()).isNotNull();
//        assertThat(repository.findById(saved.getId())).isPresent();
//    }
//
//    @Test
//    @DisplayName("같은 방의 메시지들만 조회되는지 확인")
//    void findMessagesByRoom() {
//        // given - 두 개의 다른 방에 메시지 저장
//        AiGameMessage room1Message = createTestMessage("room-1", "첫 번째 방 메시지", 1, Instant.now());
//        AiGameMessage room2Message = createTestMessage("room-2", "두 번째 방 메시지", 1, Instant.now());
//
//        repository.save(room1Message);
//        repository.save(room2Message);
//
//        // when - room-1의 메시지만 조회
//        PageRequest pageRequest = PageRequest.of(0, 10); // 최대 10개까지
//        List<AiGameMessage> result = repository.findByAiGameRoomIdOrderByCreatedAtDesc("room-1", pageRequest);
//
//        // then - room-1 메시지만 나와야 함
//        assertThat(result).hasSize(1);
//        assertThat(result.get(0).getContent()).isEqualTo("첫 번째 방 메시지");
//        assertThat(result.get(0).getAiGameRoomId()).isEqualTo("room-1");
//    }
//}