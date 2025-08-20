package org.com.dungeontalk.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.com.dungeontalk.domain.chat.common.ChatMode;
import org.com.dungeontalk.domain.chat.dto.ChatRoomDto;
import org.com.dungeontalk.domain.chat.dto.PresenceBroadcastDto;
import org.com.dungeontalk.domain.chat.dto.PresenceMessageDto;
import org.com.dungeontalk.domain.chat.dto.request.ChatRoomCreateRequestDto;
import org.com.dungeontalk.domain.chat.entity.ChatRoom;
import org.com.dungeontalk.domain.chat.repository.ChatRoomRepository;
import org.com.dungeontalk.domain.chat.util.ChatRoomProperties;
import org.com.dungeontalk.domain.member.entity.Member;
import org.com.dungeontalk.domain.member.repository.MemberRepository;
import org.com.dungeontalk.global.redis.ChatRoomMemberManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    ChatRoomRepository chatRoomRepository;

    @Mock
    ChatRoomMemberService chatRoomMemberService;

    @Mock
    ChatRoomMemberManager chatRoomMemberManager;

    @Mock
    SimpMessageSendingOperations messagingTemplate;

    @Mock
    MemberRepository memberRepository;

    @Mock
    ChatRoomProperties chatRoomProperties;

    @InjectMocks
    ChatRoomService chatRoomService;

    private ChatRoom room(String id, Integer maxCap) {
        return ChatRoom.builder()
            .id(id)
            .roomName("r-" + id)
            .mode(ChatMode.MULTI)
            .maxCapacity(maxCap)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    @Nested
    @DisplayName("createRoom: 채팅방을 생성합니다.")
    class CreateRoom {

        @Test
        @DisplayName("maxCapacity null이면 기본값을 사용해 저장한다")
        void create_usesDefaultCapacity_whenNull() {
            // given
            given(chatRoomProperties.getDefaultMaxCapacity()).willReturn(3);
            ChatRoomCreateRequestDto req = ChatRoomCreateRequestDto.builder()
                .roomName("hello")
                .mode(ChatMode.MULTI)
                .maxCapacity(null)
                .build();

            ArgumentCaptor<ChatRoom> savedCap = ArgumentCaptor.forClass(ChatRoom.class);
            given(chatRoomRepository.save(savedCap.capture()))
                .willAnswer(inv -> {
                    ChatRoom r = savedCap.getValue();
                    // id는 리포지토리가 생성해줬다고 가정
                    return ChatRoom.builder()
                        .id("RID")
                        .roomName(r.getRoomName())
                        .mode(r.getMode())
                        .maxCapacity(r.getMaxCapacity())
                        .createdAt(r.getCreatedAt())
                        .updatedAt(r.getUpdatedAt())
                        .build();
                });

            // when
            ChatRoomDto dto = chatRoomService.createRoom(req);

            // then
            assertThat(dto.getId()).isEqualTo("RID");
            assertThat(savedCap.getValue().getMaxCapacity()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("getRoomById / getAllRooms : ID를 통한 채팅방 조회, 모든 채팅방 조회")
    class Queries {
        @Test
        @DisplayName("getRoomById: 존재하면 DTO 반환")
        void getRoom_ok() {
            ChatRoom r = room("RID", 5);
            given(chatRoomRepository.findById("RID")).willReturn(Optional.of(r));

            ChatRoomDto dto = chatRoomService.getRoomById("RID");

            assertThat(dto.getId()).isEqualTo("RID");
            assertThat(dto.getMode()).isEqualTo(ChatMode.MULTI);
        }

        @Test
        @DisplayName("getRoomById: 존재하지 않으면 예외")
        void getRoom_notFound() {
            given(chatRoomRepository.findById("X")).willReturn(Optional.empty());
            assertThatThrownBy(() -> chatRoomService.getRoomById("X"))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("getAllRooms: 매핑 확인")
        void getAll() {
            given(chatRoomRepository.findAll()).willReturn(List.of(room("A", 1), room("B", 2)));
            List<ChatRoomDto> list = chatRoomService.getAllRooms();
            assertThat(list).extracting(ChatRoomDto::getId).containsExactlyInAnyOrder("A", "B");
        }
    }

    @Nested
    @DisplayName("joinRoom: 채팅방의 입장 기능을 테스트합니다.")
    class JoinRoom {

        @Test
        @DisplayName("정원 초과면 JOIN_IGNORED 브로드캐스트 후 false 반환, markOnline 호출 안 함")
        void capacityExceeded_broadcastIgnored_false() {
            String roomId = "R1";
            String memberId = "M1";
            ChatRoom r = room(roomId, 2);
            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(r));
            given(chatRoomMemberManager.getUserCount(roomId)).willReturn(2L); // 꽉 찼다
            given(chatRoomMemberManager.getOnlineNickMap(roomId)).willReturn(Map.of());
            given(chatRoomProperties.getDefaultMaxCapacity()).willReturn(2);

            chatRoomService.joinRoom(roomId, memberId);

            ArgumentCaptor<String> destCap = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object> payloadCap = ArgumentCaptor.forClass(Object.class);
            then(messagingTemplate).should().convertAndSend(destCap.capture(), payloadCap.capture());

            assertThat(destCap.getValue()).isEqualTo("/sub/chat/room/" + roomId);
            PresenceBroadcastDto payload = (PresenceBroadcastDto) payloadCap.getValue();
            assertThat(payload.getEventType()).isEqualTo("JOIN_IGNORED");

            then(chatRoomMemberService).should(never()).markOnline(anyString(), anyString());
        }

        @Test
        @DisplayName("입장 성공(SADD=1) → JOIN 브로드캐스트, markOnline 호출, true 반환")
        void join_success() {
            String roomId = "R2";
            String memberId = "M2";
            ChatRoom r = room(roomId, 3);
            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(r));
            given(chatRoomMemberManager.getUserCount(roomId)).willReturn(1L); // 아직 여유
            given(memberRepository.findById(memberId))
                .willReturn(Optional.of(Member.builder().id(memberId).nickName("닉").build()));
            given(chatRoomMemberManager.addUser(roomId, memberId, "닉")).willReturn(true);
            given(chatRoomMemberManager.getOnlineNickMap(roomId)).willReturn(Map.of(memberId, "닉"));

            boolean joined = chatRoomService.joinRoom(roomId, memberId);
            assertThat(joined).isTrue();

            then(chatRoomMemberService).should().markOnline(roomId, memberId);

            ArgumentCaptor<Object> payloadCap = ArgumentCaptor.forClass(Object.class);
            then(messagingTemplate).should().convertAndSend(eq("/sub/chat/room/" + roomId), payloadCap.capture());
            PresenceBroadcastDto payload = (PresenceBroadcastDto) payloadCap.getValue();
            assertThat(payload.getEventType()).isEqualTo("JOIN");
            assertThat(payload.getMembers())
                .extracting(PresenceMessageDto.MemberPresence::getMemberId)
                .contains(memberId);
        }

        @Test
        @DisplayName("이미 참여(SADD=0) → JOIN_IGNORED 브로드캐스트, false 반환(멱등)")
        void alreadyJoined_ignored_false() {
            String roomId = "R3";
            String memberId = "M3";
            ChatRoom r = room(roomId, 3);
            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(r));
            given(chatRoomMemberManager.getUserCount(roomId)).willReturn(1L);
            given(memberRepository.findById(memberId))
                .willReturn(Optional.of(Member.builder().id(memberId).nickName("닉3").build()));
            given(chatRoomMemberManager.addUser(roomId, memberId, "닉3")).willReturn(false);
            given(chatRoomMemberManager.getOnlineNickMap(roomId)).willReturn(Map.of(memberId, "닉3"));

            boolean joined = chatRoomService.joinRoom(roomId, memberId);
            assertThat(joined).isFalse();

            then(chatRoomMemberService).should(never()).markOnline(anyString(), anyString());
            ArgumentCaptor<Object> payloadCap = ArgumentCaptor.forClass(Object.class);
            then(messagingTemplate).should().convertAndSend(eq("/sub/chat/room/" + roomId), payloadCap.capture());
            assertThat(((PresenceBroadcastDto) payloadCap.getValue()).getEventType()).isEqualTo("JOIN_IGNORED");
        }

        @Test
        @DisplayName("무제한 방(≤0) 또는 max=null → payload의 maxCapacity=0 으로 브로드캐스트")
        void unlimitedCapacity_payloadZero() {
            String roomId = "R4"; String memberId = "M4";

            // maxCapacity가 null → 기본값도 null/<=0 이면 0 처리됨
            ChatRoom r = room(roomId, null);
            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(r));
            given(chatRoomProperties.getDefaultMaxCapacity()).willReturn(0); // 무제한으로 가정
            given(chatRoomMemberManager.getUserCount(roomId)).willReturn(0L);
            given(memberRepository.findById(memberId))
                .willReturn(Optional.of(Member.builder().id(memberId).nickName("닉4").build()));
            given(chatRoomMemberManager.addUser(roomId, memberId, "닉4")).willReturn(true);
            given(chatRoomMemberManager.getOnlineNickMap(roomId)).willReturn(Map.of(memberId, "닉4"));

            chatRoomService.joinRoom(roomId, memberId);

            ArgumentCaptor<Object> payloadCap = ArgumentCaptor.forClass(Object.class);
            then(messagingTemplate).should().convertAndSend(eq("/sub/chat/room/" + roomId), payloadCap.capture());
            PresenceBroadcastDto payload = (PresenceBroadcastDto) payloadCap.getValue();
            assertThat(payload.getMaxCapacity()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("leaveRoom: 채팅방의 퇴장 기능을 테스트합니다.")
    class LeaveRoom {

        @Test
        @DisplayName("제거 성공(SREM=1) → LEAVE 브로드캐스트, markOffline 호출")
        void leave_success() {
            String roomId = "R5"; String memberId = "M5";
            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room(roomId, 2)));
            given(chatRoomMemberManager.removeUser(roomId, memberId)).willReturn(true);
            given(chatRoomMemberManager.getUserCount(roomId)).willReturn(0L);
            given(chatRoomMemberManager.getOnlineNickMap(roomId)).willReturn(Map.of());

            boolean left = chatRoomService.leaveRoom(roomId, memberId);
            assertThat(left).isTrue();
            then(chatRoomMemberService).should().markOffline(roomId, memberId);

            ArgumentCaptor<Object> payloadCap = ArgumentCaptor.forClass(Object.class);
            then(messagingTemplate).should().convertAndSend(eq("/sub/chat/room/" + roomId), payloadCap.capture());
            assertThat(((PresenceBroadcastDto) payloadCap.getValue()).getEventType()).isEqualTo("LEAVE");
        }

        @Test
        @DisplayName("제거 실패(SREM=0) → LEAVE_IGNORED 브로드캐스트, markOffline 호출 안 함")
        void leave_ignored() {
            String roomId = "R6"; String memberId = "M6";
            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(room(roomId, 2)));
            given(chatRoomMemberManager.removeUser(roomId, memberId)).willReturn(false);
            given(chatRoomMemberManager.getUserCount(roomId)).willReturn(0L);
            given(chatRoomMemberManager.getOnlineNickMap(roomId)).willReturn(Map.of());

            boolean left = chatRoomService.leaveRoom(roomId, memberId);

            assertThat(left).isFalse();
            then(chatRoomMemberService).should(never()).markOffline(anyString(), anyString());

            ArgumentCaptor<Object> payloadCap = ArgumentCaptor.forClass(Object.class);
            then(messagingTemplate).should().convertAndSend(eq("/sub/chat/room/" + roomId), payloadCap.capture());
            assertThat(((PresenceBroadcastDto) payloadCap.getValue()).getEventType()).isEqualTo("LEAVE_IGNORED");
        }
    }

    @Nested
    @DisplayName("updateRoomCapacity: 채팅방의 최대 정원을 수정합니다.")
    class UpdateCapacity {

        @Test
        @DisplayName("현재 인원 > 새 정원(>0) 이면 예외")
        void shrinkBelowOnline_throws() {
            String roomId = "R7";
            ChatRoom r = room(roomId, 5);
            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(r));
            given(chatRoomMemberManager.getUserCount(roomId)).willReturn(3L);

            assertThatThrownBy(() -> chatRoomService.updateRoomCapacity(roomId, 2))
                .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("성공 시 저장 + CAPACITY_UPDATED 브로드캐스트")
        void update_success_broadcast() {
            String roomId = "R8";
            ChatRoom r = room(roomId, 5);
            given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(r));
            given(chatRoomMemberManager.getUserCount(roomId)).willReturn(2L);
            given(chatRoomRepository.save(any(ChatRoom.class))).willAnswer(inv -> inv.getArgument(0));
            given(chatRoomMemberManager.getOnlineNickMap(roomId)).willReturn(Map.of());

            ChatRoomDto dto = chatRoomService.updateRoomCapacity(roomId, 10);
            assertThat(dto.getId()).isEqualTo(roomId);

            ArgumentCaptor<Object> payloadCap = ArgumentCaptor.forClass(Object.class);
            then(messagingTemplate).should().convertAndSend(eq("/sub/chat/room/" + roomId), payloadCap.capture());
            assertThat(((PresenceBroadcastDto) payloadCap.getValue()).getEventType()).isEqualTo("CAPACITY_UPDATED");
        }
    }

}