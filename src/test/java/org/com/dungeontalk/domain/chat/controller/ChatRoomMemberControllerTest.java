package org.com.dungeontalk.domain.chat.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.com.dungeontalk.domain.chat.common.Status;
import org.com.dungeontalk.domain.chat.dto.MemberPresenceDto;
import org.com.dungeontalk.domain.chat.service.ChatRoomMemberService;
import org.com.dungeontalk.domain.chat.service.ChatRoomService;
import org.com.dungeontalk.global.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@WebMvcTest(controllers = ChatRoomMemberController.class)
@AutoConfigureMockMvc(addFilters = false)                       // 보안 필터 체인 비활성화
@ActiveProfiles("test")
@Import(ChatRoomMemberControllerTest.TestValidationAdvice.class)
class ChatRoomMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChatRoomService chatRoomService;

    @MockitoBean
    private ChatRoomMemberService chatRoomMemberService;

    // 보안 필터가 @Component로 등록되어 컨텍스트 로딩을 방해하지 않도록 mock 대체
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean(name = "mongoMappingContext")
    MongoMappingContext mongoMappingContext;

    @RestControllerAdvice
    static class TestValidationAdvice {
        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<String> handle(ConstraintViolationException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Validation failure");
        }
    }

    @Nested
    @DisplayName("채팅방 멤버 입장")
    class JoinApi {

        @Test
        @DisplayName("입장 성공 → HTTP 200 & RsData.resultCode=200, data=null")
        void join_success() throws Exception {
            // given
            given(chatRoomService.joinRoom("room-1", "member-1")).willReturn(true);

            // when & then
            mockMvc.perform(
                    post("/v1/chat/members/{roomId}/join/{memberId}", "room-1", "member-1")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("멤버 입장 완료"))
                .andExpect(jsonPath("$.data").doesNotExist());

            verify(chatRoomService).joinRoom("room-1", "member-1");
        }

        @Test
        @DisplayName("정원 초과/이미 참여 중 → HTTP 200 & RsData.resultCode=409, data=roomId")
        void join_conflict() throws Exception {
            // given
            given(chatRoomService.joinRoom("room-1", "member-1")).willReturn(false);

            // when & then
            mockMvc.perform(
                    post("/v1/chat/members/{roomId}/join/{memberId}", "room-1", "member-1")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk()) // 컨트롤러가 RsData로 감싸 200에 resultCode를 담음
                .andExpect(jsonPath("$.resultCode").value("409"))
                .andExpect(jsonPath("$.msg").value("정원 초과 또는 이미 참여 중"))
                .andExpect(jsonPath("$.data").value("room-1"));

            verify(chatRoomService).joinRoom("room-1", "member-1");
        }
    }

    @Nested
    @DisplayName("채팅방 멤버 퇴장")
    class LeaveApi {

        @Test
        @DisplayName("퇴장 성공 → HTTP 200 & RsData.resultCode=200, data=null")
        void leave_success() throws Exception {
            // given (void 메서드)
            given(chatRoomService.leaveRoom("room-1", "member-1")).willReturn(true);

            // when & then
            mockMvc.perform(
                    post("/v1/chat/members/{roomId}/leave/{memberId}", "room-1", "member-1")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("멤버 퇴장 완료"))
                .andExpect(jsonPath("$.data").doesNotExist());

            verify(chatRoomService).leaveRoom("room-1", "member-1");
        }
    }

    @Nested
    @DisplayName("온라인 멤버 조회")
    class OnlineMembersApi {

        @Test
        @DisplayName("목록 조회 성공 → HTTP 200 & resultCode=200 & DTO 필드(memberId, nickname, status) 검증")
        void getOnlineMembers_success() throws Exception {
            // given: 서비스가 DTO를 반환한다.
            List<MemberPresenceDto> presences = List.of(
                MemberPresenceDto.builder()
                    .memberId("m1").nickname("Alice").status(Status.ONLINE).build(),
                MemberPresenceDto.builder()
                    .memberId("m2").nickname("Bob").status(Status.ONLINE).build()
            );
            given(chatRoomMemberService.getOnlineMemberPresences("room-1"))
                .willReturn(presences);

            // when & then
            mockMvc.perform(get("/v1/chat/members/{roomId}/online", "room-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("온라인 멤버 조회 성공"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].memberId").value("m1"))
                .andExpect(jsonPath("$.data[0].nickname").value("Alice"))
                .andExpect(jsonPath("$.data[0].status").value("ONLINE"))
                .andExpect(jsonPath("$.data[1].memberId").value("m2"))
                .andExpect(jsonPath("$.data[1].nickname").value("Bob"))
                .andExpect(jsonPath("$.data[1].status").value("ONLINE"));

            verify(chatRoomMemberService).getOnlineMemberPresences("room-1");
        }

        @Test
        @DisplayName("roomId 공백/누락 시 400(Bad Request) 에러 발생")
        void getOnlineMembers_roomIdBlank() throws Exception {
            // @PathVariable @NotBlank 이므로 공백인 경우 400 에러 발생
            mockMvc.perform(get("/v1/chat/members/{roomId}/online", " "))
                .andExpect(status().isBadRequest());
            verifyNoInteractions(chatRoomMemberService);
        }
    }

}