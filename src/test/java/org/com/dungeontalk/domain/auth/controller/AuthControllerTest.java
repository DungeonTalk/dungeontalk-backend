package org.com.dungeontalk.domain.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.com.dungeontalk.domain.auth.dto.request.AuthLoginRequest;
import org.com.dungeontalk.domain.auth.dto.request.RefreshTokenRequest;
import org.com.dungeontalk.domain.auth.dto.response.JwtTokenResponse;
import org.com.dungeontalk.domain.auth.dto.response.TokenResponse;
import org.com.dungeontalk.domain.auth.service.AuthService;
import org.com.dungeontalk.global.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
    }
)
@AutoConfigureMockMvc(addFilters = false)       // 보안 필터 비활성화: 컨트롤러만 검증
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper om;

    @MockitoBean
    AuthService authService;

    @MockitoBean(name = "mongoMappingContext")
    MongoMappingContext mongoMappingContext;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("[POST] /v1/auth/login - 로그인 성공 시 accessToken Body 반환 + RT 쿠키 저장 로직 호출")
    void login_success() throws Exception {
        // given
        AuthLoginRequest req = new AuthLoginRequest("alice", "pw");
        TokenResponse token = new TokenResponse("access-123", "refresh-456");

        when(authService.login(any(AuthLoginRequest.class), any()))
            .thenReturn(token);

        // when
        var result = mockMvc.perform(
            post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req))
        );

        // then
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.msg").value("로그인 성공"))
            .andExpect(jsonPath("$.data.accessToken").value("access-123"));

        // 쿠키 저장은 service.saveRefreshTokenToCookie 가 수행 -> 호출 여부 검증
        ArgumentCaptor<HttpServletResponse> respCap = ArgumentCaptor.forClass(HttpServletResponse.class);
        verify(authService).saveRefreshTokenToCookie(respCap.capture(), eq("refresh-456"));

        // MockMvc가 넘긴 실제 응답 객체인지 대략 확인
        assertThat(respCap.getValue()).isInstanceOf(HttpServletResponse.class);

        // login 서비스 호출 검증
        verify(authService).login(any(AuthLoginRequest.class), any());
    }

//    @Test
//    @DisplayName("[POST] /v1/auth/refresh - 리프레시 성공 시 새 Access/Refresh 반환 + RT 쿠키 갱신 호출")
//    void refresh_success() throws Exception {
//        // given
//        // 필드 세터가 없으므로 리플렉션 혹은 수동 JSON 구성 사용
//        String body = """
//            {"refreshToken":"old-refresh"}
//            """;
//
//        JwtTokenResponse issued = new JwtTokenResponse("new-access", "new-refresh");
//        when(authService.refreshAccessToken("old-refresh")).thenReturn(issued);
//
//        // when
//        var result = mockMvc.perform(
//            post("/v1/auth/refresh")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(body)
//        );
//
//        // then
//        result.andExpect(status().isOk())
//            .andExpect(jsonPath("$.resultCode").value("200"))
//            .andExpect(jsonPath("$.statusCode").value(200))
//            .andExpect(jsonPath("$.msg").value("토큰 재발급 성공"))
//            .andExpect(jsonPath("$.data.accessToken").value("new-access"))
//            .andExpect(jsonPath("$.data.refreshToken").value("new-refresh"));
//
//        // 쿠키 저장 호출 검증
//        verify(authService).saveRefreshTokenToCookie(any(HttpServletResponse.class), eq("new-refresh"));
//
//        // 서비스 호출 검증
//        verify(authService).refreshAccessToken("old-refresh");
//    }

    @Test
    @DisplayName("[POST] /v1/auth/logout - 로그아웃 성공 시 쿠키 제거 호출")
    void logout_success() throws Exception {
        // given
        String accessHeader = "Bearer access-abc";
        String refreshCookie = "refresh-xyz";

        // when
        var result = mockMvc.perform(
            post("/v1/auth/logout")
                .header("Authorization", accessHeader)
                .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshCookie))
        );

        // then
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.resultCode").value("200"))
            .andExpect(jsonPath("$.statusCode").value(200))
            .andExpect(jsonPath("$.msg").value("로그아웃 완료"));


        verify(authService).logout(eq(accessHeader), eq(refreshCookie));
        verify(authService).removeRefreshTokenCookie(any(HttpServletResponse.class));
    }

}