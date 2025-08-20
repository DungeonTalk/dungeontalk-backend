package org.com.dungeontalk.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.com.dungeontalk.domain.chat.common.MessageType;
import org.com.dungeontalk.domain.chat.common.Status;
import org.com.dungeontalk.domain.chat.dto.MemberPresenceDto;
import org.com.dungeontalk.domain.chat.entity.ChatRoomMember;
import org.com.dungeontalk.domain.chat.event.ChatPresenceEvent;
import org.com.dungeontalk.domain.chat.repository.ChatRoomMemberRepository;
import org.com.dungeontalk.domain.member.entity.Member;
import org.com.dungeontalk.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ChatRoomMemberServiceTest {

    @Mock
    ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    ApplicationEventPublisher events;

    @Mock
    MemberRepository memberRepository;

    @InjectMocks
    ChatRoomMemberService chatRoomMemberService;

    private ChatRoomMember member(String roomId, String memberId, Status status) {
        return ChatRoomMember.builder()
            .roomId(roomId)
            .memberId(memberId)
            .status(status)
            .joinedAt(Instant.now().minusSeconds(60))
            .build();
    }

    @Nested
    @DisplayName("채팅방 입장에 성공하면 Online 상태로 전환, Online 상태와 관련된 테스트")
    class MarkOnline {

        @Test
        @DisplayName("신규 입장: 기록이 없으면 ONLINE으로 저장하고 JOIN 이벤트 발행, true 반환")
        void newEntry_publishJoin_returnTrue() {
            // given
            String roomId = "room-1";
            String memberId = "u-1";
            given(chatRoomMemberRepository.findByRoomIdAndMemberId(roomId, memberId))
                .willReturn(Optional.empty());
            // save 시점에 online(now) 된 엔티티가 넘어오므로 any()
            given(chatRoomMemberRepository.save(any(ChatRoomMember.class)))
                .willAnswer(inv -> inv.getArgument(0));

            // when
            boolean changed = chatRoomMemberService.markOnline(roomId, memberId);

            // then
            assertThat(changed).isTrue();
            then(chatRoomMemberRepository).should(times(1)).save(any(ChatRoomMember.class));

            ArgumentCaptor<ChatPresenceEvent> cap = ArgumentCaptor.forClass(ChatPresenceEvent.class);
            then(events).should().publishEvent(cap.capture());
            ChatPresenceEvent evt = cap.getValue();
            assertThat(evt.getRoomId()).isEqualTo(roomId);
            assertThat(evt.getMemberId()).isEqualTo(memberId);
            assertThat(evt.getType()).isEqualTo(MessageType.JOIN);
        }

        @Test
        @DisplayName("이미 ONLINE이면 이벤트 미발행, false 반환(멱등)")
        void alreadyOnline_noEvent_returnFalse() {
            // given
            String roomId = "room-1";
            String memberId = "u-1";
            ChatRoomMember existed = member(roomId, memberId, Status.ONLINE);
            given(chatRoomMemberRepository.findByRoomIdAndMemberId(roomId, memberId))
                .willReturn(Optional.of(existed));
            given(chatRoomMemberRepository.save(any(ChatRoomMember.class)))
                .willAnswer(inv -> inv.getArgument(0));

            // when
            boolean changed = chatRoomMemberService.markOnline(roomId, memberId);

            // then
            assertThat(changed).isFalse();
            then(chatRoomMemberRepository).should().save(any(ChatRoomMember.class)); // 상태 갱신 저장은 수행
            then(events).should(never()).publishEvent(any());
        }

        @Test
        @DisplayName("OFFLINE → ONLINE 전환 시 JOIN 이벤트 발행, true 반환")
        void offlineToOnline_publishJoin_returnTrue() {
            // given
            String roomId = "room-1";
            String memberId = "u-1";
            ChatRoomMember existed = member(roomId, memberId, Status.OFFLINE);
            given(chatRoomMemberRepository.findByRoomIdAndMemberId(roomId, memberId))
                .willReturn(Optional.of(existed));
            given(chatRoomMemberRepository.save(any(ChatRoomMember.class)))
                .willAnswer(inv -> inv.getArgument(0));

            // when
            boolean changed = chatRoomMemberService.markOnline(roomId, memberId);

            // then
            assertThat(changed).isTrue();
            ArgumentCaptor<ChatPresenceEvent> cap = ArgumentCaptor.forClass(ChatPresenceEvent.class);
            then(events).should().publishEvent(cap.capture());
            assertThat(cap.getValue().getType()).isEqualTo(MessageType.JOIN);
        }
    }

    @Nested
    @DisplayName("채팅방 퇴장에 성공하면 Offline 상태로 전환, Offline 상태와 관련된 테스트")
    class MarkOffline {

        @Test
        @DisplayName("ONLINE → OFFLINE 전환 시 LEAVE 이벤트 발행, true 반환")
        void onlineToOffline_publishLeave_returnTrue() {
            // given
            String roomId = "room-1";
            String memberId = "u-1";
            ChatRoomMember existed = member(roomId, memberId, Status.ONLINE);
            given(chatRoomMemberRepository.findByRoomIdAndMemberId(roomId, memberId))
                .willReturn(Optional.of(existed));
            given(chatRoomMemberRepository.save(any(ChatRoomMember.class)))
                .willAnswer(inv -> inv.getArgument(0));

            // when
            boolean changed = chatRoomMemberService.markOffline(roomId, memberId);

            // then
            assertThat(changed).isTrue();
            ArgumentCaptor<ChatPresenceEvent> cap = ArgumentCaptor.forClass(ChatPresenceEvent.class);
            then(events).should().publishEvent(cap.capture());
            assertThat(cap.getValue().getType()).isEqualTo(MessageType.LEAVE);
        }

        @Test
        @DisplayName("이미 OFFLINE이면 이벤트 미발행, false 반환")
        void alreadyOffline_noEvent_returnFalse() {
            // given
            String roomId = "room-1";
            String memberId = "u-2";
            ChatRoomMember existed = member(roomId, memberId, Status.OFFLINE);
            given(chatRoomMemberRepository.findByRoomIdAndMemberId(roomId, memberId))
                .willReturn(Optional.of(existed));

            // when
            boolean changed = chatRoomMemberService.markOffline(roomId, memberId);

            // then
            assertThat(changed).isFalse();
            then(chatRoomMemberRepository).should(never()).save(any(ChatRoomMember.class));
            then(events).should(never()).publishEvent(any());
        }

        @Test
        @DisplayName("기록이 없으면 아무 것도 하지 않고 false 반환")
        void notExists_returnFalse() {
            // given
            String roomId = "room-1";
            String memberId = "u-3";
            given(chatRoomMemberRepository.findByRoomIdAndMemberId(roomId, memberId))
                .willReturn(Optional.empty());

            // when
            boolean changed = chatRoomMemberService.markOffline(roomId, memberId);

            // then
            assertThat(changed).isFalse();
            then(chatRoomMemberRepository).should(never()).save(any(ChatRoomMember.class));
            then(events).should(never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("getOnlineMembers / getOnlineMemberPresences")
    class Queries {

        @Test
        @DisplayName("getOnlineMembers: ONLINE 상태만 필터링해 반환")
        void getOnlineMembers_onlyOnline() {
            // given
            String roomId = "room-1";
            List<ChatRoomMember> all = List.of(
                member(roomId, "u1", Status.ONLINE),
                member(roomId, "u2", Status.OFFLINE),
                member(roomId, "u3", Status.ONLINE)
            );
            given(chatRoomMemberRepository.findByRoomId(roomId)).willReturn(all);

            // when
            List<ChatRoomMember> result = chatRoomMemberService.getOnlineMembers(roomId);

            // then
            assertThat(result).extracting(ChatRoomMember::getMemberId)
                .containsExactlyInAnyOrder("u1", "u3");
        }

        @Test
        @DisplayName("getOnlineMemberPresences: memberId → nickname 매핑, 없으면 '알 수 없음'")
        void getOnlineMemberPresences_mapsNickname() {
            // given
            String roomId = "room-1";
            List<ChatRoomMember> all = List.of(
                member(roomId, "u1", Status.ONLINE),
                member(roomId, "u2", Status.ONLINE)
            );
            given(chatRoomMemberRepository.findByRoomId(roomId)).willReturn(all);

            // u1만 존재, u2는 미존재 → '알 수 없음'
            Member m1 = Member.builder()
                .id("u1")
                .nickName("닉1")
                .build();

            given(memberRepository.findByIdIn(List.of("u1","u2"))).willReturn(List.of(m1));

            // when
            List<MemberPresenceDto> presences = chatRoomMemberService.getOnlineMemberPresences(roomId);

            // then
            assertThat(presences).hasSize(2);
            assertThat(presences).anySatisfy(p -> {
                if (p.getMemberId().equals("u1")) assertThat(p.getNickname()).isEqualTo("닉1");
            });
            assertThat(presences).anySatisfy(p -> {
                if (p.getMemberId().equals("u2")) assertThat(p.getNickname()).isEqualTo("알 수 없음");
            });
        }
    }

    @Nested
    @DisplayName("기타 예외 및 발생할 수 있는 추가 케이스")
    class AdditionalCases {

        @Test
        @DisplayName("markOnline: 저장된 엔티티는 ONLINE 상태여야 한다")
        void markOnline_savedEntityIsOnline() {
            // given
            String roomId = "room-A"; String memberId = "m-1";
            given(chatRoomMemberRepository.findByRoomIdAndMemberId(roomId, memberId))
                .willReturn(Optional.empty());

            ArgumentCaptor<ChatRoomMember> savedCap = ArgumentCaptor.forClass(ChatRoomMember.class);
            given(chatRoomMemberRepository.save(savedCap.capture()))
                .willAnswer(inv -> inv.getArgument(0));

            // when
            boolean changed = chatRoomMemberService.markOnline(roomId, memberId);

            // then
            assertThat(changed).isTrue();
            ChatRoomMember saved = savedCap.getValue();
            assertThat(saved.getRoomId()).isEqualTo(roomId);
            assertThat(saved.getMemberId()).isEqualTo(memberId);
            assertThat(saved.getStatus()).isEqualTo(Status.ONLINE);
        }

        @Test
        @DisplayName("markOffline: ONLINE → 저장되는 엔티티는 OFFLINE 상태여야 한다")
        void markOffline_savedEntityIsOffline() {
            // given
            String roomId = "room-A"; String memberId = "m-2";
            ChatRoomMember existed = member(roomId, memberId, Status.ONLINE);
            given(chatRoomMemberRepository.findByRoomIdAndMemberId(roomId, memberId))
                .willReturn(Optional.of(existed));

            ArgumentCaptor<ChatRoomMember> savedCap = ArgumentCaptor.forClass(ChatRoomMember.class);
            given(chatRoomMemberRepository.save(savedCap.capture()))
                .willAnswer(inv -> inv.getArgument(0));

            // when
            boolean changed = chatRoomMemberService.markOffline(roomId, memberId);

            // then
            assertThat(changed).isTrue();
            ChatRoomMember saved = savedCap.getValue();
            assertThat(saved.getStatus()).isEqualTo(Status.OFFLINE);
        }

        @Test
        @DisplayName("getOnlineMembers: 모두 OFFLINE이면 빈 리스트를 반환한다")
        void getOnlineMembers_allOffline_returnsEmpty() {
            // given
            String roomId = "room-Z";
            List<ChatRoomMember> all = List.of(
                member(roomId, "u1", Status.OFFLINE),
                member(roomId, "u2", Status.OFFLINE)
            );
            given(chatRoomMemberRepository.findByRoomId(roomId)).willReturn(all);

            // when
            List<ChatRoomMember> result = chatRoomMemberService.getOnlineMembers(roomId);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getOnlineMemberPresences: memberId 목록은 distinct 되어 MemberRepository가 1회 호출된다")
        void getOnlineMemberPresences_distinctsIds() {
            // given
            String roomId = "room-D";
            // 동일 memberId가 두 개 있어도 distinct 후 ["u1","u2"] 로 1번만 조회
            List<ChatRoomMember> all = List.of(
                member(roomId, "u1", Status.ONLINE),
                member(roomId, "u1", Status.ONLINE),
                member(roomId, "u2", Status.ONLINE)
            );
            given(chatRoomMemberRepository.findByRoomId(roomId)).willReturn(all);
            given(memberRepository.findByIdIn(any()))
                .willReturn(List.of(Member.builder()
                    .id("u1")
                    .nickName("닉1")
                    .build()));

            ArgumentCaptor<List<String>> idsCap = ArgumentCaptor.forClass(List.class);

            // when
            chatRoomMemberService.getOnlineMemberPresences(roomId);

            // then
            then(memberRepository).should(times(1)).findByIdIn(idsCap.capture());
            List<String> queried = idsCap.getValue();
            assertThat(queried).containsExactlyInAnyOrder("u1", "u2"); // 중복 제거 확인
        }
    }

}