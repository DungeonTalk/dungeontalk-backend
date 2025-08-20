package org.com.dungeontalk.domain.chat.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.com.dungeontalk.domain.chat.dto.ChatMessageDto;
import org.com.dungeontalk.domain.chat.dto.ChatRoomDto;
import org.com.dungeontalk.domain.chat.dto.response.ChatMessageResponse;
import org.com.dungeontalk.domain.chat.service.ChatMessageService;
import org.com.dungeontalk.domain.chat.service.ChatRoomService;
import org.com.dungeontalk.global.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@WebMvcTest(controllers = ChatRoomController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChatRoomService chatRoomService;

    @MockitoBean
    private ChatMessageService chatMessageService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean(name = "mongoMappingContext")
    MongoMappingContext mongoMappingContext;

    @Nested
    @DisplayName("채팅방 생성/조회/입장/퇴장")
    class RoomApis {

        @Test
        @DisplayName("채팅방 생성 성공 - 200 & RsData 구조 검증")
        void createRoom_success() throws Exception {
            // given
            ChatRoomDto mockRoom = Mockito.mock(ChatRoomDto.class);
            given(chatRoomService.createRoom(any())).willReturn(mockRoom);

            Map<String, Object> body = new HashMap<>();
            body.put("roomName", "테스트방");
            body.put("mode", "MULTI");
            body.put("maxCapacity", 3);

            // when & then
            mockMvc.perform(
                    post("/v1/chat/room")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("채팅방 생성 완료"))
                .andExpect(jsonPath("$.data").exists());
        }

        @Test
        @DisplayName("채팅방 단일 조회 성공 - 200")
        void getRoom_success() throws Exception {
            // given
            ChatRoomDto mockRoom = Mockito.mock(ChatRoomDto.class);
            given(chatRoomService.getRoomById("room-1")).willReturn(mockRoom);

            // when & then
            mockMvc.perform(get("/v1/chat/room/{roomId}", "room-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("채팅방 조회 성공"))
                .andExpect(jsonPath("$.data").exists());
        }

        @Test
        @DisplayName("채팅방 전체 조회 성공 - 200")
        void getAllRooms_success() throws Exception {
            // given
            ChatRoomDto mockRoom1 = Mockito.mock(ChatRoomDto.class);
            ChatRoomDto mockRoom2 = Mockito.mock(ChatRoomDto.class);
            given(chatRoomService.getAllRooms()).willReturn(List.of(mockRoom1, mockRoom2));

            // when & then
            mockMvc.perform(get("/v1/chat/room"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("전체 채팅방 조회 성공"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
        }

        @Test
        @DisplayName("채팅방 입장 성공 - 200 & data에 roomId 반환")
        void joinRoom_success() throws Exception {
            // given
            given(chatRoomService.joinRoom("room-1", "member-1")).willReturn(true);

            // when & then
            mockMvc.perform(post("/v1/chat/room/{roomId}/join/{memberId}", "room-1", "member-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("채팅방 입장 성공"))
                .andExpect(jsonPath("$.data").value("room-1"));

            // 호출 검증
            verify(chatRoomService).joinRoom("room-1", "member-1");
        }

        @Test
        @DisplayName("채팅방 퇴장 성공 - 200 & data에 roomId 반환")
        void leaveRoom_success() throws Exception {
            // given
            given(chatRoomService.leaveRoom("room-1", "member-1")).willReturn(true);

            // when & then
            mockMvc.perform(delete("/v1/chat/room/{roomId}/leave/{memberId}", "room-1", "member-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("채팅방 퇴장 성공"))
                .andExpect(jsonPath("$.data").value("room-1"));

            // 실제 호출됐는지 검증
            verify(chatRoomService).leaveRoom("room-1", "member-1");
        }
    }

    @Nested
    @DisplayName("메시지 전송/조회")
    class MessageApis {

        @Test
        @DisplayName("메시지 전송 실패 - body에 roomId 누락 시 Validation 400 발생")
        void sendMessage_roomIdMissing_validationError() throws Exception {
            // given
            Map<String, Object> body = new HashMap<>();
            body.put("type", "TALK");
            body.put("senderId", "member-1");
            body.put("content", "hello");

            // when & then
            mockMvc.perform(
                    post("/v1/chat/room/{roomId}/message", "room-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                )
                .andExpect(status().isBadRequest());    // Bean Validation이 바로 400

            // 서비스는 호출되지 않아야 함
            verifyNoInteractions(chatMessageService);
        }

        @Test
        @DisplayName("메시지 전송 실패 - path roomId와 body roomId 불일치 시 400(RsData) 에러 발생")
        void sendMessage_missingRoomId() throws Exception {
            // given: body에 roomId 없음
            Map<String, Object> body = new HashMap<>();
            body.put("roomId", "room-2");       // path는 room-1
            body.put("type", "TALK");
            body.put("senderId", "member-1");
            body.put("content", "hello");

            mockMvc.perform(
                    post("/v1/chat/room/{roomId}/message", "room-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                )
                .andExpect(status().isOk()) // 컨트롤러가 RsData로 감싸서 200에 resultCode "400"을 담음
                .andExpect(jsonPath("$.resultCode").value("400"))
                .andExpect(jsonPath("$.msg").value("요청 경로의 roomId와 body의 roomId가 일치하지 않습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());

            // 이 케이스도 서비스는 호출되지 않아야 함 (컨트롤러에서 조기 반환)
            verifyNoInteractions(chatMessageService);
        }

        @Test
        @DisplayName("메시지 전송 성공 - 200")
        void sendMessage_success() throws Exception {
            // given
            ChatMessageDto mockMsg = Mockito.mock(ChatMessageDto.class);
            given(chatMessageService.processMessage(any())).willReturn(mockMsg);

            Map<String, Object> body = new HashMap<>();
            body.put("roomId", "room-1");
            body.put("type", "TALK");      // JOIN/LEAVE도 가능
            body.put("senderId", "member-1");
            body.put("content", "안녕하세요");

            mockMvc.perform(
                    post("/v1/chat/room/{roomId}/message", "room-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("메시지 전송 성공"))
                .andExpect(jsonPath("$.data").exists());
        }

        @Test
        @DisplayName("메시지 목록 조회 성공 - 기본 최신순 정렬(createdAt desc)로 페이징")
        void getMessages_success() throws Exception {
            // given
            var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC,
                "createdAt"));
            ChatMessageResponse msg1 = Mockito.mock(ChatMessageResponse.class);
            ChatMessageResponse msg2 = Mockito.mock(ChatMessageResponse.class);

            given(chatMessageService.getMessagesByRoomId(anyString(), any()))
                .willReturn(new PageImpl<>(List.of(msg1, msg2), pageable, 2));

            // when & then
            mockMvc.perform(get("/v1/chat/room/{roomId}/messages?page=0&size=20&sort=createdAt,desc", "room-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("채팅 메시지 목록 조회 성공"))
                // Spring Data Page 직렬화 기본 구조(content/totalElements/totalPages/size/number 등)을 가정
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2));
        }
    }

}