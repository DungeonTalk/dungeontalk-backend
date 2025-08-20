package org.com.dungeontalk.domain.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.com.dungeontalk.domain.member.dto.request.RegisterRequest;
import org.com.dungeontalk.domain.member.dto.response.RegisterResponse;
import org.com.dungeontalk.domain.member.dto.response.UserWithCharacterInfoResponse;
import org.com.dungeontalk.domain.member.service.MemberService;
import org.com.dungeontalk.global.security.CustomUserDetails;
import org.com.dungeontalk.global.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@WebMvcTest(controllers = MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(MemberControllerTest.SecurityArgResolverConfig.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    MemberService memberService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean(name = "mongoMappingContext")
    MongoMappingContext mongoMappingContext;

    @TestConfiguration(proxyBeanMethods = false)
    static class SecurityArgResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Test
    @DisplayName("회원가입 성공 → 200 & RsData(msg, data) 구조 확인")
    void register_success() throws Exception {
        // given
        RegisterRequest request = new RegisterRequest("testId", "testNick", "plainPw");
        RegisterResponse response = new RegisterResponse("M-001", "testId", "testNick");
        given(memberService.register(any(RegisterRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(
                post("/v1/member/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.msg").value("회원가입이 정상적으로 완료되었습니다"))
            .andExpect(jsonPath("$.data.id").value("M-001"))
            .andExpect(jsonPath("$.data.name").value("testId"))
            .andExpect(jsonPath("$.data.nickName").value("testNick"));
    }

    @Test
    @DisplayName("회원의 캐릭터 정보 조회")
    void getUserInfo_success() throws Exception {
        // 1) principal 목과 스텁
        CustomUserDetails cud = mock(CustomUserDetails.class);
        when(cud.getId()).thenReturn("M-001");

        // 2) Authentication 구성 (principal = cud)
        Authentication auth =
            new UsernamePasswordAuthenticationToken(cud, null, List.of());

        // 3) 서비스 스텁
        when(memberService.getUserWithCharacterInfo("M-001"))
            .thenReturn(UserWithCharacterInfoResponse.of("nickname", true));

        // 4) 호출 & 검증
        mockMvc.perform(get("/v1/member/status/me").with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nickname").value("nickname"))
            .andExpect(jsonPath("$.data.meta.isExistCharacter").value(true));
    }
}

