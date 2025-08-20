package org.com.dungeontalk.domain.auth.controller;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static org.hamcrest.Matchers.anyOf;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.com.dungeontalk.domain.auth.service.ValkeyService;
import org.com.dungeontalk.global.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ValkeyController.class)
@AutoConfigureMockMvc(addFilters = false)
class ValkeyControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ValkeyService valkeyService;

    @MockitoBean(name = "mongoMappingContext")
    MongoMappingContext mongoMappingContext;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("[GET] /v1/valkey/session/test/save - 테스트 키 저장")
    void saveTestSessionData() throws Exception {
        // when
        var result = mockMvc.perform(get("/v1/valkey/session/test/save")
            .accept(MediaType.TEXT_PLAIN));

        // then
        result.andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("테스트 세션 데이터 저장: ")));

        // 컨트롤러가 내부에서 saveSessionData(key, value)를 1회 호출함 → 이 상호작용을 기대/검증
        verify(valkeyService, times(1)).saveSessionData(anyString(), anyString());
        verifyNoMoreInteractions(valkeyService);
    }

    @Test
    @DisplayName("[GET] /v1/valkey/session/all - 세션 전체 키/값 조회")
    void getAllSessionData() throws Exception {
        Map<String, Instant> map = new LinkedHashMap<>();
        map.put("k1", Instant.parse("2025-01-01T00:00:00Z"));
        map.put("k2", Instant.parse("2025-01-02T00:00:00Z"));

        when(valkeyService.getAllSessionKeyValues()).thenReturn(map);

        var result = mockMvc.perform(get("/v1/valkey/session/all")
            .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
            // Instant는 ISO-8601 문자열로 직렬화됨
            .andExpect(jsonPath("$.k1").value("2025-01-01T00:00:00Z"))
            .andExpect(jsonPath("$.k2").value("2025-01-02T00:00:00Z"));

        verify(valkeyService).getAllSessionKeyValues();
    }

    @Test
    @DisplayName("[GET] /v1/valkey/session/keys - 세션 모든 키만 조회")
    void getAllSessionKeys() throws Exception {
        when(valkeyService.getAllSessionKeys()).thenReturn(Set.of("a", "b"));

        var result = mockMvc.perform(get("/v1/valkey/session/keys")
            .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
            .andExpect(jsonPath("$[0]").value(anyOf(
                org.hamcrest.Matchers.is("a"),
                org.hamcrest.Matchers.is("b")
            )))
            .andExpect(jsonPath("$[1]").value(anyOf(
                org.hamcrest.Matchers.is("a"),
                org.hamcrest.Matchers.is("b")
            )));

        verify(valkeyService).getAllSessionKeys();
    }

}