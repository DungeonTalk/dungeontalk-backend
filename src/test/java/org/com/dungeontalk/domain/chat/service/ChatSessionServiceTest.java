package org.com.dungeontalk.domain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.com.dungeontalk.domain.auth.service.ValkeyService;
import org.com.dungeontalk.domain.chat.common.Status;
import org.com.dungeontalk.domain.chat.dto.ChatSessionDto;
import org.com.dungeontalk.domain.chat.entity.ChatRoom;
import org.com.dungeontalk.domain.chat.repository.ChatRoomRepository;
import org.com.dungeontalk.global.exception.customException.ChatException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatSessionServiceTest {

    @Mock
    private ValkeyService valkeyService;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ChatSessionService chatSessionService;

    private ChatRoom testRoom;
    private static final String TEST_ROOM_ID = "test-room-id";
    private static final String TEST_MEMBER_ID = "test-member-id";
    private static final String TEST_NICKNAME = "테스트유저";
    private static final String TEST_WEBSOCKET_SESSION_ID = "ws-session-123";

    @BeforeEach
    void setUp() {
        testRoom = ChatRoom.builder()
                .id(TEST_ROOM_ID)
                .roomName("테스트 채팅방")
                .maxCapacity(10)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("세션 시작 - 정상 케이스")
    void startSession_Success() throws Exception {
        // given
        when(chatRoomRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.of(testRoom));
        when(valkeyService.exists(anyString())).thenReturn(true);

        // when
        ChatSessionDto result = chatSessionService.startSession(
                TEST_ROOM_ID, TEST_MEMBER_ID, TEST_NICKNAME, TEST_WEBSOCKET_SESSION_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getRoomId()).isEqualTo(TEST_ROOM_ID);
        assertThat(result.getMemberId()).isEqualTo(TEST_MEMBER_ID);
        assertThat(result.getNickname()).isEqualTo(TEST_NICKNAME);
        assertThat(result.getStatus()).isEqualTo(Status.ONLINE);
        assertThat(result.getJoinedAt()).isNotNull();
        assertThat(result.getLastActivity()).isNotNull();
        assertThat(result.getWebsocketSessionId()).isEqualTo(TEST_WEBSOCKET_SESSION_ID);
    }

    @Test
    @DisplayName("세션 시작 - 채팅방이 존재하지 않는 경우")
    void startSession_RoomNotFound() {
        // given
        when(chatRoomRepository.findById(TEST_ROOM_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatSessionService.startSession(
                TEST_ROOM_ID, TEST_MEMBER_ID, TEST_NICKNAME, TEST_WEBSOCKET_SESSION_ID))
                .isInstanceOf(ChatException.class);

        verify(chatRoomRepository).findById(TEST_ROOM_ID);
        verify(valkeyService, never()).setWithExpiration(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("세션 연장 - 정상 케이스")
    void extendSession_Success() throws Exception {
        // given
        ChatSessionDto existingSession = ChatSessionDto.builder()
                .roomId(TEST_ROOM_ID)
                .memberId(TEST_MEMBER_ID)
                .nickname(TEST_NICKNAME)
                .status(Status.ONLINE)
                .joinedAt(Instant.now().minusSeconds(3600))
                .lastActivity(Instant.now().minusSeconds(600))
                .websocketSessionId(TEST_WEBSOCKET_SESSION_ID)
                .build();

        String sessionJson = "{\"roomId\":\"" + TEST_ROOM_ID + "\"}";

        when(valkeyService.exists(anyString())).thenReturn(true);
        when(valkeyService.get(anyString())).thenReturn(sessionJson);
        when(objectMapper.readValue(eq(sessionJson), eq(ChatSessionDto.class))).thenReturn(existingSession);

        // when & then (메서드 호출이 예외 없이 완료되는지 확인)
        assertThatCode(() -> chatSessionService.extendSession(TEST_ROOM_ID, TEST_MEMBER_ID))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("세션 연장 - 세션이 존재하지 않는 경우")
    void extendSession_SessionNotExists() {
        // given
        when(valkeyService.exists(anyString())).thenReturn(false);

        // when
        chatSessionService.extendSession(TEST_ROOM_ID, TEST_MEMBER_ID);

        // then
        verify(valkeyService).exists(anyString());
        verify(valkeyService, never()).get(anyString());
        verify(valkeyService, never()).setWithExpiration(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("Heartbeat 처리 - 정상 케이스")
    void heartbeat_Success() {
        // given
        when(valkeyService.exists(anyString())).thenReturn(true);
        doNothing().when(valkeyService).expire(anyString(), anyInt());

        // when
        boolean result = chatSessionService.heartbeat(TEST_ROOM_ID, TEST_MEMBER_ID);

        // then
        assertThat(result).isTrue();
        verify(valkeyService).exists(anyString());
        verify(valkeyService).expire(anyString(), anyInt());
    }

    @Test
    @DisplayName("Heartbeat 처리 - 세션이 존재하지 않는 경우")
    void heartbeat_SessionNotExists() {
        // given
        when(valkeyService.exists(anyString())).thenReturn(false);

        // when
        boolean result = chatSessionService.heartbeat(TEST_ROOM_ID, TEST_MEMBER_ID);

        // then
        assertThat(result).isFalse();
        verify(valkeyService).exists(anyString());
        verify(valkeyService, never()).expire(anyString(), anyInt());
    }

    @Test
    @DisplayName("세션 종료 - 정상 케이스")
    void endSession_Success() {
        // given
        when(valkeyService.exists(anyString())).thenReturn(true);
        doNothing().when(valkeyService).delete(anyString());

        // when
        chatSessionService.endSession(TEST_ROOM_ID, TEST_MEMBER_ID);

        // then
        verify(valkeyService).exists(anyString());
        verify(valkeyService).delete(anyString());
    }

    @Test
    @DisplayName("세션 종료 - 세션이 이미 없는 경우")
    void endSession_SessionNotExists() {
        // given
        when(valkeyService.exists(anyString())).thenReturn(false);

        // when
        chatSessionService.endSession(TEST_ROOM_ID, TEST_MEMBER_ID);

        // then
        verify(valkeyService).exists(anyString());
        verify(valkeyService, never()).delete(anyString());
    }

    @Test
    @DisplayName("세션 유효성 검증 - 유효한 세션")
    void isSessionValid_Valid() {
        // given
        when(valkeyService.exists(anyString())).thenReturn(true);

        // when
        boolean result = chatSessionService.isSessionValid(TEST_ROOM_ID, TEST_MEMBER_ID);

        // then
        assertThat(result).isTrue();
        verify(valkeyService).exists(anyString());
    }

    @Test
    @DisplayName("세션 유효성 검증 - 유효하지 않은 세션")
    void isSessionValid_Invalid() {
        // given
        when(valkeyService.exists(anyString())).thenReturn(false);

        // when
        boolean result = chatSessionService.isSessionValid(TEST_ROOM_ID, TEST_MEMBER_ID);

        // then
        assertThat(result).isFalse();
        verify(valkeyService).exists(anyString());
    }
}
