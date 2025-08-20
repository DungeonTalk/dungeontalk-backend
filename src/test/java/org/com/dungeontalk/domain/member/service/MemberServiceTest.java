package org.com.dungeontalk.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import org.com.dungeontalk.domain.gamecharacter.repository.GameCharacterRepository;
import org.com.dungeontalk.domain.member.dto.request.RegisterRequest;
import org.com.dungeontalk.domain.member.dto.response.RegisterResponse;
import org.com.dungeontalk.domain.member.entity.Member;
import org.com.dungeontalk.domain.member.repository.MemberRepository;
import org.com.dungeontalk.global.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService Unit Test")
class MemberServiceTest {

    @Mock
    BCryptPasswordEncoder passwordEncoder;

    @Mock
    MemberRepository memberRepository;

    @Mock
    GameCharacterRepository gameCharacterRepository;

    @Mock
    JwtService jwtService;

    @InjectMocks
    MemberService memberService;

    private RegisterRequest newRegisterRequest() {
        return new RegisterRequest("testId", "testNick", "plainPw");
    }

    private Member newMember() {
        return Member.builder()
            .id("M-001")
            .name("testId")
            .nickName("testNick")
            .password("encodedPw")
            .build();
    }

    @Test
    @DisplayName("아이디/닉네임 중복 없으면 회원가입 성공")
    void register_success() {
        // given
        RegisterRequest req = newRegisterRequest();

        given(memberRepository.findByName(req.name())).willReturn(Optional.empty());
        given(memberRepository.findByNickName(req.nickName())).willReturn(Optional.empty());
        given(passwordEncoder.encode(req.password())).willReturn("encodedPw");

        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> {
                Member member = invocation.getArgument(0);
                ReflectionTestUtils.setField(member, "id", "M-001");    // 강제로 id 부여
                return member;
            });

        // when
        RegisterResponse res = memberService.register(req);

        // then
        assertThat(res.id()).isEqualTo("M-001");
        assertThat(res.name()).isEqualTo("testId");
        assertThat(res.nickName()).isEqualTo("testNick");

        verify(memberRepository).save(any(Member.class));
        verify(passwordEncoder).encode("plainPw");
    }

    @Test
    @DisplayName("이미 존재하는 아이디면 예외 발생")
    void duplicate_name_throwsException() {
        // given
        RegisterRequest req = newRegisterRequest();
        given(memberRepository.findByName(req.name()))
            .willReturn(Optional.of(newMember()));

        // when & then
        assertThatThrownBy(() -> memberService.register(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("이미 존재하는 아이디입니다.");
    }

    @Test
    @DisplayName("이미 존재하는 닉네임이면 예외 발생")
    void duplicate_nickName_throwsException() {
        // given
        RegisterRequest req = newRegisterRequest();
        given(memberRepository.findByName(req.name())).willReturn(Optional.empty());
        given(memberRepository.findByNickName(req.nickName()))
            .willReturn(Optional.of(newMember()));

        // when & then
        assertThatThrownBy(() -> memberService.register(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("이미 존재하는 닉네임입니다.");
    }

}