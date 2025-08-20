package org.com.dungeontalk.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.member.dto.request.RegisterRequest;
import org.com.dungeontalk.domain.member.dto.response.RegisterResponse;
import org.com.dungeontalk.domain.member.dto.response.UserWithCharacterInfoResponse;
import org.com.dungeontalk.domain.member.entity.Member;
import org.com.dungeontalk.domain.member.service.MemberService;
import org.com.dungeontalk.global.rsData.RsData;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.com.dungeontalk.global.security.CustomUserDetails;

@Tag(name = "회원 관리", description = "회원 관련 API")
@Slf4j
@RestController
@RequestMapping("/v1/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * 회원가입 컨트롤러
     * @param registerRequest - 회원가입 요청 DTO
     */
    @Operation(summary = "회원가입", description = "새로운 회원을 등록합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200-1", description = "회원가입 성공",
            content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
        @ApiResponse(responseCode = "400", description = "이미 존재하는 아이디 또는 닉네임")
    })
    @PostMapping("/register")
    public RsData<RegisterResponse> register(@RequestBody @Valid RegisterRequest registerRequest) {
        RegisterResponse registerResponse = memberService.register(registerRequest);
        return RsData.of("회원가입이 정상적으로 완료되었습니다", registerResponse);
    }

    /**
     * 현재 로그인한 사용자의 정보 및 캐릭터 존재 여부 조회
     * JWT 토큰에서 사용자 정보를 추출하여 조회
     */
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보와 캐릭터 존재 여부를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = UserWithCharacterInfoResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 멤버")
    })
    @SecurityRequirement(name = "JWT")
    @GetMapping("/status/me")
    public RsData<UserWithCharacterInfoResponse> getUserInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        String memberId = userDetails.getId();
        UserWithCharacterInfoResponse userWithCharacterInfo = memberService.getUserWithCharacterInfo(memberId);
        return RsData.of("200", "사용자 정보 조회 완료", userWithCharacterInfo);
    }

}
