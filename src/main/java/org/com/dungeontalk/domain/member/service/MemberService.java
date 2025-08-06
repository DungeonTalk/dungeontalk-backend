package org.com.dungeontalk.domain.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.member.dto.request.RegisterRequest;
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

    /**
     * 회원가입 메서드
     * @param registerRequest 유저의 회원가입 요청 DTO
     * @return 회원가입 된 유저 정보 객체
     */
    @Transactional
    public Member register(RegisterRequest registerRequest) {

        /* Name 중복 체크 추가하기 */

        /* NickName 중복 체크 추가하기 */

        /* UUID 7 적용하기 */
        String id = "sfsdf"; // 일단 하드 코딩

        String encodedPassword = passwordEncoder.encode(registerRequest.password());
        Member member = registerRequest.toEntity(id,encodedPassword);

        return memberRepository.save(member);
    }


}
