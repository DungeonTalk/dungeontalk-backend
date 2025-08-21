//package org.com.dungeontalk.domain.aichat.service;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.BDDMockito.then;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.com.dungeontalk.domain.aichat.common.AiMessageType;
//import org.com.dungeontalk.domain.aichat.dto.AiGameMessageDto;
//import org.com.dungeontalk.domain.aichat.dto.request.AiMessageSaveRequest;
//import org.com.dungeontalk.domain.aichat.entity.AiGameMessage;
//import org.com.dungeontalk.domain.aichat.repository.AiGameMessageRepository;
//import org.com.dungeontalk.domain.aichat.repository.AiGameRoomRepository;
//import org.com.dungeontalk.domain.aichat.util.AiGameValidator;
//import org.com.dungeontalk.domain.aichat.service.AiGameRoomService;
//import org.com.dungeontalk.domain.aichat.service.AiGameStateService;
//import org.com.dungeontalk.global.filter.ProfanityFilterService;
//import org.com.dungeontalk.global.filter.config.ProfanityFilterProperties;
//import org.com.dungeontalk.global.redis.RedisPublisher;
//import org.springframework.context.ApplicationEventPublisher;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.time.Instant;
//import java.util.Collections;
//
//@ExtendWith(MockitoExtension.class)
//class AiGameMessageServiceTest {
//
//    @Mock
//    SimpMessagingTemplate messagingTemplate;
//
//    @Mock
//    AiGameMessageRepository aiGameMessageRepository;
//
//    @Mock
//    AiGameRoomRepository aiGameRoomRepository;
//
//    @Mock
//    AiGameValidator aiGameValidator;
//
//    @Mock
//    RedisPublisher redisPublisher;
//
//    @Mock
//    ObjectMapper objectMapper;
//
//    @Mock
//    ApplicationEventPublisher eventPublisher;
//
//    @Mock
//    AiGameRoomService aiGameRoomService;
//
//    @Mock
//    AiGameStateService aiGameStateService;
//
//    @Mock
//    ProfanityFilterService profanityFilterService;
//
//    @Mock
//    ProfanityFilterProperties profanityFilterProperties;
//
//    @InjectMocks
//    AiGameMessageService aiGameMessageService;
//
//    @Test
//    @DisplayName("AI 메시지 저장 성공 테스트")
//    void saveAiMessage_success() {
//        // given
//        AiMessageSaveRequest request = AiMessageSaveRequest.builder()
//                .aiGameRoomId("room-123")
//                .gameId("game-456")
//                .content("AI 응답 메시지")
//                .turnNumber(1)
//                .build();
//
//        // Mock: getNextMessageOrder 결과
//        given(aiGameMessageRepository.findMaxMessageOrderByTurn("room-123", 1))
//                .willReturn(Collections.emptyList());
//
//        // Mock: 저장된 메시지 반환
//        AiGameMessage savedMessage = AiGameMessage.builder()
//                .id("saved-id")
//                .aiGameRoomId("room-123")
//                .gameId("game-456")
//                .senderId("AI_GM")
//                .senderNickname("던전 마스터")
//                .content("AI 응답 메시지")
//                .messageType(AiMessageType.AI)
//                .turnNumber(1)
//                .messageOrder(1)
//                .createdAt(Instant.now())
//                .build();
//
//        given(aiGameMessageRepository.save(any(AiGameMessage.class)))
//                .willReturn(savedMessage);
//
//        // when
//        AiGameMessageDto result = aiGameMessageService.saveAiMessage(request);
//
//        // then
//        assertThat(result).isNotNull();
//        assertThat(result.getContent()).isEqualTo("AI 응답 메시지");
//        assertThat(result.getMessageType()).isEqualTo(AiMessageType.AI);
//        assertThat(result.getTurnNumber()).isEqualTo(1);
//
//        // Mock 호출 검증
//        then(aiGameValidator).should().validateGameRoom("room-123");
//        then(aiGameMessageRepository).should().save(any(AiGameMessage.class));
//    }
//
//    @Test
//    @DisplayName("메시지 순서 계산 테스트")
//    void messageOrder_test() {
//        // given - 첫 번째 메시지인 경우
//        AiMessageSaveRequest request = AiMessageSaveRequest.builder()
//                .aiGameRoomId("room-123")
//                .gameId("game-456")
//                .content("첫 번째 메시지")
//                .turnNumber(1)
//                .build();
//
//        // Mock: 기존 메시지가 없는 경우
//        given(aiGameMessageRepository.findMaxMessageOrderByTurn("room-123", 1))
//                .willReturn(Collections.emptyList());
//
//        AiGameMessage savedMessage = AiGameMessage.builder()
//                .id("saved-id")
//                .aiGameRoomId("room-123")
//                .messageOrder(1) // 첫 번째 메시지이므로 1
//                .turnNumber(1)
//                .content("첫 번째 메시지")
//                .messageType(AiMessageType.AI)
//                .createdAt(Instant.now())
//                .build();
//
//        given(aiGameMessageRepository.save(any(AiGameMessage.class)))
//                .willReturn(savedMessage);
//
//        // when
//        AiGameMessageDto result = aiGameMessageService.saveAiMessage(request);
//
//        // then
//        assertThat(result.getMessageOrder()).isEqualTo(1);
//        then(aiGameMessageRepository).should().findMaxMessageOrderByTurn("room-123", 1);
//    }
//}