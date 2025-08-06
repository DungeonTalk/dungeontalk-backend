package org.com.dungeontalk.domain.member.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.member.dto.request.RegisterRequest;
import org.com.dungeontalk.domain.member.dto.response.RegisterResponse;
import org.com.dungeontalk.domain.member.entity.Member;
import org.com.dungeontalk.domain.member.service.MemberService;
import org.com.dungeontalk.global.rsData.RsData;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/register")
    public RsData<RegisterResponse> register(@RequestBody @Valid RegisterRequest registerRequest) {
        RegisterResponse registerResponse = memberService.register(registerRequest);
        return RsData.of("회원가입이 정상적으로 완료되었습니다", registerResponse);
    }

}
