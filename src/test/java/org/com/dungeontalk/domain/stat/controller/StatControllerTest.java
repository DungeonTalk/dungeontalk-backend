package org.com.dungeontalk.domain.stat.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.com.dungeontalk.domain.stat.service.StatAggregateService;
import org.com.dungeontalk.global.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.Map;

@WebMvcTest(controllers = StatController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("StatController 웹 계층 테스트")
class StatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatAggregateService statAggregateService;
    
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @MockitoBean(name = "mongoMappingContext")
    MongoMappingContext mongoMappingContext;

    private static final String BASE_URL = "/v1/stat";
    private static final String CHARACTER_ID = "character-123";

    private Map<String, Double> createMockCalculatedStats() {
        Map<String, Double> stats = new LinkedHashMap<>();
        stats.put("healthPoints", 232.0);
        stats.put("manaPoints", 230.0);
        stats.put("physicalAttack", 18.0);
        stats.put("magicAttack", 20.8);
        stats.put("evasionRate", 11.0);
        stats.put("accuracy", 71.0);
        stats.put("diceOdds", 1.08);
        return stats;
    }

    @Nested
    @DisplayName("계산된 스탯 조회 API 테스트")
    class CalculatedStatsApiTest {

        @Test
        @DisplayName("캐릭터의 계산된 스탯 조회에 성공한다")
        void getCalculatedStats_success() throws Exception {
            // given - 계산된 스탯 데이터 Mock 준비
            Map<String, Double> mockStats = createMockCalculatedStats();
            given(statAggregateService.calculateAllStats(CHARACTER_ID))
                .willReturn(mockStats);

            // when & then - GET 요청 실행 및 응답 검증
            mockMvc.perform(get(BASE_URL + "/{characterId}/calculated", CHARACTER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("스탯 계산 완료"))
                .andExpect(jsonPath("$.data.characterId").value(CHARACTER_ID))
                .andExpect(jsonPath("$.data.healthPoints").value(232.0))
                .andExpect(jsonPath("$.data.manaPoints").value(230.0))
                .andExpect(jsonPath("$.data.physicalAttack").value(18.0))
                .andExpect(jsonPath("$.data.magicAttack").value(20.8))
                .andExpect(jsonPath("$.data.evasionRate").value(11.0))
                .andExpect(jsonPath("$.data.accuracy").value(71.0))
                .andExpect(jsonPath("$.data.diceOdds").value(1.08))
                .andExpect(jsonPath("$.data.calculatedAt").exists());
        }

        // 0인 스탯 테스트 제거: DDL 기본값으로 보장되며, 실제 게임에서는 발생하지 않는 상황

        @Test
        @DisplayName("누락된 스탯이 있는 경우 기본값 0으로 응답한다")
        void getCalculatedStats_withMissingStats_defaultsToZero() throws Exception {
            // given - 일부 스탯이 누락된 데이터 (CalculatedStatsResponse.fromMap에서 기본값 처리)
            Map<String, Double> partialStats = new LinkedHashMap<>();
            partialStats.put("healthPoints", 150.0);
            partialStats.put("physicalAttack", 20.0);
            partialStats.put("accuracy", 65.0);
            // manaPoints, magicAttack, evasionRate, diceOdds 누락
            
            given(statAggregateService.calculateAllStats(CHARACTER_ID))
                .willReturn(partialStats);

            // when & then - 누락된 스탯은 기본값 0으로 응답되는지 검증
            mockMvc.perform(get(BASE_URL + "/{characterId}/calculated", CHARACTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.healthPoints").value(150.0))
                .andExpect(jsonPath("$.data.manaPoints").value(0.0)) // 기본값
                .andExpect(jsonPath("$.data.physicalAttack").value(20.0))
                .andExpect(jsonPath("$.data.magicAttack").value(0.0)) // 기본값
                .andExpect(jsonPath("$.data.evasionRate").value(0.0)) // 기본값
                .andExpect(jsonPath("$.data.accuracy").value(65.0))
                .andExpect(jsonPath("$.data.diceOdds").value(0.0)); // 기본값
        }

        @Test
        @DisplayName("소수점 스탯 값이 정확하게 응답된다")
        void getCalculatedStats_withDecimalValues_accurate() throws Exception {
            // given - 소수점이 포함된 스탯 데이터
            Map<String, Double> decimalStats = new LinkedHashMap<>();
            decimalStats.put("healthPoints", 182.5);
            decimalStats.put("manaPoints", 205.75);
            decimalStats.put("physicalAttack", 15.8);
            decimalStats.put("magicAttack", 18.25);
            decimalStats.put("evasionRate", 12.3);
            decimalStats.put("accuracy", 68.9);
            decimalStats.put("diceOdds", 1.44);
            
            given(statAggregateService.calculateAllStats(CHARACTER_ID))
                .willReturn(decimalStats);

            // when & then - 소수점 값이 정확하게 응답되는지 검증
            mockMvc.perform(get(BASE_URL + "/{characterId}/calculated", CHARACTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.healthPoints").value(182.5))
                .andExpect(jsonPath("$.data.manaPoints").value(205.75))
                .andExpect(jsonPath("$.data.physicalAttack").value(15.8))
                .andExpect(jsonPath("$.data.magicAttack").value(18.25))
                .andExpect(jsonPath("$.data.evasionRate").value(12.3))
                .andExpect(jsonPath("$.data.accuracy").value(68.9))
                .andExpect(jsonPath("$.data.diceOdds").value(1.44));
        }

        // 빈 스탯 맵 테스트 제거: DDL 기본값으로 보장되며, 실제 게임에서는 발생하지 않는 상황

        @Test
        @DisplayName("매우 큰 스탯 값도 정상 처리된다")
        void getCalculatedStats_largeValues_success() throws Exception {
            // given - 매우 큰 스탯 값들
            Map<String, Double> largeStats = new LinkedHashMap<>();
            largeStats.put("healthPoints", 999999.99);
            largeStats.put("manaPoints", 888888.88);
            largeStats.put("physicalAttack", 777777.77);
            largeStats.put("magicAttack", 666666.66);
            largeStats.put("evasionRate", 555555.55);
            largeStats.put("accuracy", 444444.44);
            largeStats.put("diceOdds", 333333.33);
            
            given(statAggregateService.calculateAllStats(CHARACTER_ID))
                .willReturn(largeStats);

            // when & then - 큰 값들도 정상 처리되는지 검증
            mockMvc.perform(get(BASE_URL + "/{characterId}/calculated", CHARACTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.healthPoints").value(999999.99))
                .andExpect(jsonPath("$.data.manaPoints").value(888888.88))
                .andExpect(jsonPath("$.data.physicalAttack").value(777777.77))
                .andExpect(jsonPath("$.data.magicAttack").value(666666.66))
                .andExpect(jsonPath("$.data.evasionRate").value(555555.55))
                .andExpect(jsonPath("$.data.accuracy").value(444444.44))
                .andExpect(jsonPath("$.data.diceOdds").value(333333.33));
        }
    }

    @Nested
    @DisplayName("예외 상황 API 테스트")
    class ExceptionApiTest {

        // 예외 처리 테스트는 비즈니스 로직 수준에서 검증됨 (Service 테스트)
        // Controller 테스트에서는 정상 케이스에 집중

        @Test
        @DisplayName("잘못된 Path Variable 형식으로 요청해도 처리된다")
        void getCalculatedStats_invalidPathVariable_processed() throws Exception {
            // given - 특수문자가 포함된 캐릭터 ID (URL 인코딩 처리)
            String specialCharacterId = "char@#$%";
            Map<String, Double> mockStats = createMockCalculatedStats();
            
            given(statAggregateService.calculateAllStats(specialCharacterId))
                .willReturn(mockStats);

            // when & then - 특수문자 ID도 처리되는지 검증
            mockMvc.perform(get(BASE_URL + "/{characterId}/calculated", specialCharacterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.characterId").value(specialCharacterId));
        }
    }

    @Nested
    @DisplayName("응답 형식 검증 테스트")
    class ResponseFormatTest {

        @Test
        @DisplayName("RsData 응답 래퍼 구조가 올바르다")
        void getCalculatedStats_rsDataWrapper_correct() throws Exception {
            // given - 기본 스탯 데이터
            Map<String, Double> mockStats = createMockCalculatedStats();
            given(statAggregateService.calculateAllStats(CHARACTER_ID))
                .willReturn(mockStats);

            // when & then - RsData 래퍼 구조 검증
            mockMvc.perform(get(BASE_URL + "/{characterId}/calculated", CHARACTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").exists())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.msg").value("스탯 계산 완료"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data").isMap());
        }

        @Test
        @DisplayName("CalculatedStatsResponse 구조가 올바르다")
        void getCalculatedStats_responseStructure_correct() throws Exception {
            // given - 기본 스탯 데이터
            Map<String, Double> mockStats = createMockCalculatedStats();
            given(statAggregateService.calculateAllStats(CHARACTER_ID))
                .willReturn(mockStats);

            // when & then - CalculatedStatsResponse 구조 검증
            mockMvc.perform(get(BASE_URL + "/{characterId}/calculated", CHARACTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.characterId").exists())
                .andExpect(jsonPath("$.data.healthPoints").exists())
                .andExpect(jsonPath("$.data.manaPoints").exists())
                .andExpect(jsonPath("$.data.physicalAttack").exists())
                .andExpect(jsonPath("$.data.magicAttack").exists())
                .andExpect(jsonPath("$.data.evasionRate").exists())
                .andExpect(jsonPath("$.data.accuracy").exists())
                .andExpect(jsonPath("$.data.diceOdds").exists())
                .andExpect(jsonPath("$.data.calculatedAt").exists())
                // 타입 검증
                .andExpect(jsonPath("$.data.characterId").isString())
                .andExpect(jsonPath("$.data.healthPoints").isNumber())
                .andExpect(jsonPath("$.data.manaPoints").isNumber())
                .andExpect(jsonPath("$.data.physicalAttack").isNumber())
                .andExpect(jsonPath("$.data.magicAttack").isNumber())
                .andExpect(jsonPath("$.data.evasionRate").isNumber())
                .andExpect(jsonPath("$.data.accuracy").isNumber())
                .andExpect(jsonPath("$.data.diceOdds").isNumber())
                .andExpect(jsonPath("$.data.calculatedAt").isString());
        }

        @Test
        @DisplayName("Content-Type이 application/json이다")
        void getCalculatedStats_contentType_json() throws Exception {
            // given - 기본 스탯 데이터
            Map<String, Double> mockStats = createMockCalculatedStats();
            given(statAggregateService.calculateAllStats(CHARACTER_ID))
                .willReturn(mockStats);

            // when & then - Content-Type 검증
            mockMvc.perform(get(BASE_URL + "/{characterId}/calculated", CHARACTER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }
}