package org.com.dungeontalk.domain.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.member.dto.request.RegisterRequest;
import org.com.dungeontalk.domain.member.dto.response.RegisterResponse;
import org.com.dungeontalk.domain.member.entity.Member;
import org.com.dungeontalk.domain.member.repository.MemberRepository;
import org.com.dungeontalk.global.security.JwtService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final BCryptPasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final JwtService jwtService;

    // 회원가입 메서드
    @Transactional
    public RegisterResponse register(RegisterRequest registerRequest) {

        if (memberRepository.findByName(registerRequest.name()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        if (memberRepository.findByNickName(registerRequest.nickName()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 닉네임입니다.");
        }

        String encodedPassword = passwordEncoder.encode(registerRequest.password());
        Member member = registerRequest.toEntity(encodedPassword);
        memberRepository.save(member);

        return new RegisterResponse(member.getId(), member.getName(), member.getNickName());
    }




}
