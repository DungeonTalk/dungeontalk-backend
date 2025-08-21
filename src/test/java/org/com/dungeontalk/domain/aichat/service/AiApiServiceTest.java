//package org.com.dungeontalk.domain.aichat.service;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.ThrowableAssert.catchThrowable;
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.BDDMockito.given;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import java.util.List;
//import java.util.Map;
//import org.com.dungeontalk.domain.aichat.common.AiMessageType;
//import org.com.dungeontalk.domain.aichat.dto.AiGameMessageDto;
//import org.com.dungeontalk.domain.aichat.dto.response.AiServiceResponse;
//import org.com.dungeontalk.global.exception.ErrorCode;
//import org.com.dungeontalk.global.exception.customException.AiChatException;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.test.util.ReflectionTestUtils;
//import org.springframework.web.client.ResourceAccessException;
//import org.springframework.web.client.RestTemplate;
//
//@ExtendWith(MockitoExtension.class)
//class AiApiServiceTest {
//
//    @Mock
//    RestTemplate restTemplate;
//
//    ObjectMapper objectMapper = new ObjectMapper();
//
//    @InjectMocks
//    AiApiService service;
//
//    @BeforeEach
//    void setUp() {
//        // @Value 주입 대체
//        ReflectionTestUtils.setField(service, "aiServiceUrl", "http://localhost:8001");
//        ReflectionTestUtils.setField(service, "aiServiceTimeout", 60000);
//    }
//
//    private List<AiGameMessageDto> sampleContext() {
//        // 실제 프로젝트의 DTO에 맞게 생성하세요.
//        // 아래는 예시: messageType, senderNickname, content, turnNumber, messageOrder 등
//        AiGameMessageDto m1 = AiGameMessageDto.builder()
//            .messageType(AiMessageType.USER)     // 프로젝트 타입에 맞게 수정
//            .senderNickname("Alice")
//            .content("Hello")
//            .turnNumber(1)
//            .messageOrder(1)
//            .build();
//
//        AiGameMessageDto m2 = AiGameMessageDto.builder()
//            .messageType(AiMessageType.AI)       // 프로젝트 타입에 맞게 수정
//            .senderNickname("Narrator")
//            .content("Welcome")
//            .turnNumber(1)
//            .messageOrder(2)
//            .build();
//
//        return List.of(m1, m2);
//    }
//
//    @Nested
//    @DisplayName("AI 반응 테스트")
//    class GenerateAiResponseTest {
//
//        @Test
//        @DisplayName("200 OK + content 존재 → AiServiceResponse 매핑 성공")
//        void ok_withContent_returnsResponse() {
//            // given
//            Map<String, Object> body = Map.of(
//                "content", "AI says hi",
//                "response_time", 123L,
//                "sources", List.of("doc1", "doc2")
//            );
//
//            given(restTemplate.exchange(
//                eq("http://localhost:8001/ai-response"),
//                eq(HttpMethod.POST),
//                any(HttpEntity.class),
//                eq(Map.class)
//            )).willReturn(ResponseEntity.ok(body));
//
//            // when
//            AiServiceResponse res = service.generateAiResponse(
//                "game-1", "room-1", "user-1", "hello", sampleContext(), 3
//            );
//
//            // then
//            assertThat(res.getContent()).isEqualTo("AI says hi");
//            assertThat(res.getResponseTime()).isEqualTo(123L);
//            assertThat(res.getSources()).containsExactly("doc1", "doc2");
//        }
//
//        @Test
//        @DisplayName("200 OK + content 공백인 경우 AI_RESPONSE_PROCESSING_ERROR 발생")
//        void ok_withBlankContent_throwsProcessingError() {
//            // given
//            Map<String, Object> body = Map.of(
//                "content", "   ",            // 공백
//                "response_time", 50L
//            );
//            given(restTemplate.exchange(
//                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)
//            )).willReturn(ResponseEntity.ok(body));
//
//            // when
//            Throwable thrown = catchThrowable(() -> service.generateAiResponse(
//                "game-1", "room-1", "user-1", "hello", sampleContext(), 1
//            ));
//
//            // then
//            assertThat(thrown).isInstanceOf(AiChatException.class);
//            AiChatException ex = (AiChatException) thrown;
//            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AI_RESPONSE_PROCESSING_ERROR);
//        }
//
//        @Test
//        @DisplayName("HTTP 500 등 비정상 상태인 경우 AI_RESPONSE_PROCESSING_ERROR 발생")
//        void errorStatus_throwsProcessingError() {
//            given(restTemplate.exchange(
//                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)
//            )).willReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
//
//            Throwable thrown = catchThrowable(() -> service.generateAiResponse(
//                "game-1", "room-1", "user-1", "hello", sampleContext(), 2
//            ));
//
//            assertThat(thrown).isInstanceOf(AiChatException.class);
//            assertThat(((AiChatException) thrown).getErrorCode())
//                .isEqualTo(ErrorCode.AI_RESPONSE_PROCESSING_ERROR);
//        }
//
//        @Test
//        @DisplayName("네트워크/타임아웃(ResourceAccessException)인 경우 타임아웃 에러 발생")
//        void timeout_throwsTimeoutError() {
//            given(restTemplate.exchange(
//                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)
//            )).willThrow(new ResourceAccessException("timeout"));
//
//            Throwable thrown = catchThrowable(() -> service.generateAiResponse(
//                "game-1", "room-1", "user-1", "hello", sampleContext(), 5
//            ));
//
//            assertThat(thrown).isInstanceOf(AiChatException.class);
//            assertThat(((AiChatException) thrown).getErrorCode())
//                .isEqualTo(ErrorCode.AI_RESPONSE_TIMEOUT_ERROR);
//        }
//    }
//
//    @Nested
//    @DisplayName("AI health check - AI 상태 관련 테스트")
//    class AiServiceHealthyTest {
//
//        @Test
//        @DisplayName("status=healthy → true")
//        void healthy_true() {
//            given(restTemplate.getForEntity("http://localhost:8001/health", Map.class))
//                .willReturn(ResponseEntity.ok(Map.of("status", "healthy")));
//
//            boolean result = service.isAiServiceHealthy();
//
//            assertThat(result).isTrue();
//        }
//
//        @Test
//        @DisplayName("status!=healthy → false")
//        void notHealthy_false() {
//            given(restTemplate.getForEntity("http://localhost:8001/health", Map.class))
//                .willReturn(ResponseEntity.ok(Map.of("status", "unhealthy")));
//
//            assertThat(service.isAiServiceHealthy()).isFalse();
//        }
//
//        @Test
//        @DisplayName("HTTP 200 아님 → false")
//        void nonOk_false() {
//            given(restTemplate.getForEntity("http://localhost:8001/health", Map.class))
//                .willReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
//
//            assertThat(service.isAiServiceHealthy()).isFalse();
//        }
//
//        @Test
//        @DisplayName("예외 발생 시 → false")
//        void exception_false() {
//            given(restTemplate.getForEntity("http://localhost:8001/health", Map.class))
//                .willThrow(new ResourceAccessException("timeout"));
//
//            assertThat(service.isAiServiceHealthy()).isFalse();
//        }
//    }
//
//}