package org.com.dungeontalk.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.com.dungeontalk.domain.chat.common.MessageType;
import org.com.dungeontalk.domain.chat.dto.ChatMessageDto;
import org.com.dungeontalk.domain.chat.dto.request.ChatMessageSendRequestDto;
import org.com.dungeontalk.domain.chat.dto.response.ChatMessageResponse;
import org.com.dungeontalk.domain.chat.entity.ChatMessage;
import org.com.dungeontalk.domain.chat.repository.ChatMessageRepository;
import org.com.dungeontalk.domain.member.entity.Member;
import org.com.dungeontalk.domain.member.repository.MemberRepository;
import org.com.dungeontalk.global.exception.ErrorCode;
import org.com.dungeontalk.global.exception.customException.ChatException;
import org.com.dungeontalk.global.redis.RedisPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    ChatMessageRepository chatMessageRepository;

    @Mock
    MemberRepository memberRepository;

    @Mock
    RedisPublisher redisPublisher;

    @Mock
    ObjectMapper objectMapper;

    @Mock
    ChatRoomService chatRoomService;

    @InjectMocks
    ChatMessageService chatMessageService;

    private ChatMessageSendRequestDto talkReq(String roomId, String senderId, String content) {
        return ChatMessageSendRequestDto.builder()
            .roomId(roomId)
            .senderId(senderId)
            .content(content)
            .messageId("m-123")       // 명시적으로 작성하여 의존 회피
            .type(MessageType.TALK)
            .build();
    }

    @Nested
    @DisplayName("메시지의 브로드캐스트 관련한 테스트")
    class ProcessMessage {

        @Test
        @DisplayName("TALK: 저장 후 Redis로 브로드캐스트하고 DTO 반환")
        void process_TALK_publishAndReturn() throws Exception {
            // given
            ChatMessageSendRequestDto dto = talkReq("room-1", "u-1", "hello");

            Member sender = mock(Member.class);
            given(sender.getNickName()).willReturn("Neo");
            given(memberRepository.findById("u-1")).willReturn(Optional.of(sender));

            // save 호출 시 저장된 엔티티 그대로 반환
            given(chatMessageRepository.save(any(ChatMessage.class)))
                .willAnswer(inv -> inv.getArgument(0));

            given(objectMapper.writeValueAsString(any(ChatMessageDto.class)))
                .willReturn("{json}");

            // when
            ChatMessageDto result = chatMessageService.processMessage(dto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getSenderNickname()).isEqualTo("Neo");
            then(redisPublisher).should().publish(eq("room-1"), eq("{json}"));
            then(chatRoomService).should(never()).joinRoom(anyString(), anyString());
            then(chatRoomService).should(never()).leaveRoom(anyString(), anyString());
        }

        @Test
        @DisplayName("JOIN: 입장 처리 위임, 브로드캐스트 없음")
        void process_JOIN_delegateToJoin() throws Exception {
            // given
            ChatMessageSendRequestDto dto = ChatMessageSendRequestDto.builder()
                .roomId("r")
                .senderId("u")
                .type(MessageType.JOIN)
                .build();

            // when
            ChatMessageDto result = chatMessageService.processMessage(dto);

            // then
            assertThat(result).isNull();
            then(chatRoomService).should().joinRoom("r", "u");
            then(redisPublisher).shouldHaveNoInteractions();
            then(chatMessageRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("LEAVE: 퇴장 처리 위임, 브로드캐스트 없음")
        void process_LEAVE_delegateToLeave() throws Exception {
            ChatMessageSendRequestDto dto = ChatMessageSendRequestDto.builder()
                .roomId("r")
                .senderId("u")
                .type(MessageType.LEAVE)
                .build();

            ChatMessageDto result = chatMessageService.processMessage(dto);

            assertThat(result).isNull();
            then(chatRoomService).should().leaveRoom("r", "u");
            then(redisPublisher).shouldHaveNoInteractions();
            then(chatMessageRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("PRESENCE: 무시하고 null 반환")
        void process_PRESENCE_ignore() throws Exception {
            ChatMessageSendRequestDto dto = ChatMessageSendRequestDto.builder()
                .roomId("r")
                .senderId("u")
                .type(MessageType.PRESENCE)
                .build();

            ChatMessageDto result = chatMessageService.processMessage(dto);

            assertThat(result).isNull();
            then(redisPublisher).shouldHaveNoInteractions();
            then(chatRoomService).shouldHaveNoInteractions();
            then(chatMessageRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("dto=null -> CHAT_INVALID_PAYLOAD 예외")
        void process_nullPayload_throws() {
            assertThatThrownBy(() -> chatMessageService.processMessage(null))
                .isInstanceOf(ChatException.class)
                .satisfies(ex -> assertThat(((ChatException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_INVALID_PAYLOAD));
        }

        @Test
        @DisplayName("type=null -> CHAT_INVALID_MESSAGE_TYPE 예외")
        void process_nullType_throws() {
            ChatMessageSendRequestDto dto = ChatMessageSendRequestDto.builder()
                .roomId("r").senderId("u").content("x").type(null).build();

            assertThatThrownBy(() -> chatMessageService.processMessage(dto))
                .isInstanceOf(ChatException.class)
                .satisfies(ex -> assertThat(((ChatException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_INVALID_MESSAGE_TYPE));
        }
    }

    @Nested
    @DisplayName("채팅방에 접속한 회원들의 메시지 송수신 및 상태 확인 테스트")
    class HandleTalkMessage {

        @Test
        @DisplayName("roomId/senderId/content 없으면 CHAT_INVALID_PAYLOAD")
        void invalidPayload_throws() {
            ChatMessageSendRequestDto dto = ChatMessageSendRequestDto.builder()
                .roomId(null).senderId("sender-1").content("  ").type(MessageType.TALK).build();

            assertThatThrownBy(() -> chatMessageService.handleTalkMessage(dto))
                .isInstanceOf(ChatException.class)
                .satisfies(ex -> assertThat(((ChatException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_INVALID_PAYLOAD));
        }

        @Test
        @DisplayName("보낸 사람을 찾지 못하면 CHAT_MEMBER_NOT_FOUND")
        void senderNotFound_throws() {
            ChatMessageSendRequestDto dto = talkReq("room-1", "missing", "hi");
            given(memberRepository.findById("missing")).willReturn(Optional.empty());

            assertThatThrownBy(() -> chatMessageService.handleTalkMessage(dto))
                .isInstanceOf(ChatException.class)
                .satisfies(ex -> assertThat(((ChatException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.CHAT_MEMBER_NOT_FOUND));
        }

        @Test
        @DisplayName("정상 처리 시 저장 + 닉네임 포함 DTO 반환")
        void ok_returnsDto() {
            ChatMessageSendRequestDto dto = talkReq("r", "u", "hi");

            Member sender = mock(Member.class);
            given(sender.getNickName()).willReturn("Neo");
            given(memberRepository.findById("u")).willReturn(Optional.of(sender));
            given(chatMessageRepository.save(any(ChatMessage.class)))
                .willAnswer(inv -> inv.getArgument(0));

            ChatMessageDto out = chatMessageService.handleTalkMessage(dto);

            assertThat(out.getRoomId()).isEqualTo("r");
            assertThat(out.getSenderId()).isEqualTo("u");
            assertThat(out.getContent()).isEqualTo("hi");
            assertThat(out.getSenderNickname()).isEqualTo("Neo");
        }
    }

    @Nested
    @DisplayName("getMessagesByRoomId: 방 ID를 통한 메시지 확인 테스트")
    class GetMessagesByRoomId {

        @Test
        @DisplayName("메시지를 통해 닉네임 매핑하여 반환(존재하지 않는 닉네임은 '알 수 없음' 으로 표시)")
        void mapsNicknames() {
            // given page
            ChatMessage m1 = mock(ChatMessage.class);
            given(m1.getMessageId()).willReturn("m1");
            given(m1.getRoomId()).willReturn("r");
            given(m1.getSenderId()).willReturn("u1");
            given(m1.getContent()).willReturn("a");
            given(m1.getCreatedAt()).willReturn(Instant.now().minusSeconds(2));

            ChatMessage m2 = mock(ChatMessage.class);
            given(m2.getMessageId()).willReturn("m2");
            given(m2.getRoomId()).willReturn("r");
            given(m2.getSenderId()).willReturn("u2"); // 닉네임 없는 경우
            given(m2.getContent()).willReturn("b");
            given(m2.getCreatedAt()).willReturn(Instant.now());

            List<ChatMessage> list = List.of(m1, m2);
            Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
            Page<ChatMessage> page = new PageImpl<>(list, pageable, list.size());

            given(chatMessageRepository.findByRoomId("r", pageable)).willReturn(page);

            // member nick map: u1만 존재, u2는 누락
            Member u1 = mock(Member.class);
            given(u1.getId()).willReturn("u1");
            given(u1.getNickName()).willReturn("Neo");
            given(memberRepository.findByIdIn(List.of("u1","u2")))
                .willReturn(List.of(u1));

            // when
            Page<ChatMessageResponse> result = chatMessageService.getMessagesByRoomId("r", pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent().get(0).getSenderId()).isIn("u1","u2");
            assertThat(result.getContent())
                .extracting(ChatMessageResponse::getSenderNickname)
                .containsExactlyInAnyOrder("Neo", "알 수 없음");
        }
    }

}