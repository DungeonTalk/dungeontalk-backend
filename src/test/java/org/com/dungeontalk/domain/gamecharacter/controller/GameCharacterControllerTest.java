package org.com.dungeontalk.domain.gamecharacter.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.com.dungeontalk.domain.gamecharacter.dto.request.AddExperienceRequest;
import org.com.dungeontalk.domain.gamecharacter.dto.request.CreateCharacterRequest;
import org.com.dungeontalk.domain.gamecharacter.dto.response.GameCharacterDetailResponse;
import org.com.dungeontalk.domain.gamecharacter.dto.response.GameCharacterResponse;
import org.com.dungeontalk.domain.gamecharacter.service.GameCharacterService;
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

import java.time.Instant;
import java.util.List;

@WebMvcTest(controllers = GameCharacterController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("GameCharacterController 웹 계층 테스트")
class GameCharacterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GameCharacterService gameCharacterService;
    
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @MockitoBean(name = "mongoMappingContext")
    MongoMappingContext mongoMappingContext;

    private static final String BASE_URL = "/v1/characters";
    private static final String MEMBER_ID = "member-123";
    private static final String CHARACTER_ID = "character-123";
    private static final String RACE_ID = "race-123";

    private GameCharacterResponse createMockGameCharacterResponse() {
        return new GameCharacterResponse(
            CHARACTER_ID,
            MEMBER_ID,
            RACE_ID,
            1,
            0L,
            0,
            10,
            10,
            10,
            10,
            10,
            10,
            Instant.now(),
            Instant.now()
        );
    }

    private GameCharacterDetailResponse createMockGameCharacterDetailResponse() {
        return new GameCharacterDetailResponse(
            CHARACTER_ID,
            MEMBER_ID,
            "테스터",
            RACE_ID,
            "인간",
            1,
            0L,
            0,
            10,
            10,
            10,
            10,
            10,
            10,
            200.0,
            200.0,
            15.0,
            13.0,
            10.0,
            70.0,
            1.2,
            Instant.now(),
            Instant.now()
        );
    }

    @Nested
    @DisplayName("캐릭터 생성 API 테스트")
    class CreateCharacterApiTest {

        @Test
        @DisplayName("정상적인 캐릭터 생성 요청 시 201 응답을 반환한다")
        void createCharacter_success() throws Exception {
            // given - 캐릭터 생성 요청 데이터와 Mock 응답 준비
            CreateCharacterRequest request = new CreateCharacterRequest(MEMBER_ID, RACE_ID);
            GameCharacterResponse mockResponse = createMockGameCharacterResponse();
            
            given(gameCharacterService.createCharacter(any(CreateCharacterRequest.class)))
                .willReturn(mockResponse);

            // when & then - POST 요청 실행 및 응답 검증
            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("201"))
                .andExpect(jsonPath("$.msg").value("캐릭터 생성 완료"))
                .andExpect(jsonPath("$.data.id").value(CHARACTER_ID))
                .andExpect(jsonPath("$.data.memberId").value(MEMBER_ID))
                .andExpect(jsonPath("$.data.playerLevel").value(1))
                .andExpect(jsonPath("$.data.strength").value(10));
        }

        // 예외 처리 테스트는 GlobalExceptionHandler에서 처리하므로 생략
        // 비즈니스 로직 예외는 Service 레이어 테스트에서 검증됨

        @Test
        @DisplayName("잘못된 JSON 형식 요청 시 400 응답을 반환한다")
        void createCharacter_invalidJson_badRequest() throws Exception {
            // given - 잘못된 JSON 형식의 요청 데이터
            String invalidJson = "{\"memberId\": \"" + MEMBER_ID + "\", \"raceId\":}";

            // when & then - 잘못된 JSON에 대한 400 응답 검증
            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("캐릭터 조회 API 테스트")
    class GetCharacterApiTest {

        @Test
        @DisplayName("캐릭터 기본 정보 조회 시 200 응답을 반환한다")
        void getCharacterBasic_success() throws Exception {
            // given - 캐릭터 기본 정보 조회 Mock 응답 준비
            GameCharacterResponse mockResponse = createMockGameCharacterResponse();
            given(gameCharacterService.findById(CHARACTER_ID)).willReturn(mockResponse);

            // when & then - GET 요청 실행 및 응답 검증
            mockMvc.perform(get(BASE_URL + "/basic/{id}", CHARACTER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("캐릭터 조회 완료"))
                .andExpect(jsonPath("$.data.id").value(CHARACTER_ID))
                .andExpect(jsonPath("$.data.memberId").value(MEMBER_ID));
        }

        // 예외 처리 테스트는 비즈니스 로직 수준에서 검증됨 (Service 테스트)

        @Test
        @DisplayName("캐릭터 상세 정보 조회 시 200 응답을 반환한다")
        void getCharacterDetail_success() throws Exception {
            // given - 캐릭터 상세 정보 조회 Mock 응답 준비
            GameCharacterDetailResponse mockResponse = createMockGameCharacterDetailResponse();
            given(gameCharacterService.findDetailById(CHARACTER_ID)).willReturn(mockResponse);

            // when & then - 상세 정보 조회 요청 및 응답 검증
            mockMvc.perform(get(BASE_URL + "/{id}", CHARACTER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("캐릭터 상세 조회 완료"))
                .andExpect(jsonPath("$.data.id").value(CHARACTER_ID))
                .andExpect(jsonPath("$.data.nickname").value("테스터"))
                .andExpect(jsonPath("$.data.raceName").value("인간"))
                .andExpect(jsonPath("$.data.healthPoints").value(200.0))
                .andExpect(jsonPath("$.data.manaPoints").value(200.0));
        }

        @Test
        @DisplayName("멤버별 캐릭터 조회 시 200 응답을 반환한다")
        void getCharacterByMember_success() throws Exception {
            // given - 멤버별 캐릭터 조회 Mock 응답 준비
            GameCharacterResponse mockResponse = createMockGameCharacterResponse();
            given(gameCharacterService.findByMemberId(MEMBER_ID)).willReturn(mockResponse);

            // when & then - 멤버 ID로 캐릭터 조회 요청 및 응답 검증
            mockMvc.perform(get(BASE_URL)
                    .param("memberId", MEMBER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("멤버 캐릭터 조회 완료"))
                .andExpect(jsonPath("$.data.memberId").value(MEMBER_ID));
        }

        // 예외 처리 테스트는 비즈니스 로직 수준에서 검증됨 (Service 테스트)
    }

    @Nested
    @DisplayName("유틸리티 API 테스트")
    class UtilityApiTest {

        @Test
        @DisplayName("종족 목록 조회 시 200 응답을 반환한다")
        void getRaces_success() throws Exception {
            // given - 종족 목록 Mock 응답 준비
            List<String> mockRaces = List.of("인간", "엘프", "드워프");
            given(gameCharacterService.getRaces()).willReturn(mockRaces);

            // when & then - 종족 목록 조회 요청 및 응답 검증
            mockMvc.perform(get(BASE_URL + "/races"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("종족 목록 조회 완료"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0]").value("인간"))
                .andExpect(jsonPath("$.data[1]").value("엘프"))
                .andExpect(jsonPath("$.data[2]").value("드워프"));
        }

        @Test
        @DisplayName("캐릭터 존재 여부 확인 시 200 응답을 반환한다")
        void hasCharacter_success() throws Exception {
            // given - 캐릭터 존재 여부 확인 Mock 응답 준비
            given(gameCharacterService.hasCharacter(MEMBER_ID)).willReturn(true);
            given(gameCharacterService.hasCharacter("no-character-member")).willReturn(false);

            // when & then - 캐릭터가 있는 멤버 검증
            mockMvc.perform(get(BASE_URL + "/exists")
                    .param("memberId", MEMBER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("캐릭터 존재 여부 확인 완료"))
                .andExpect(jsonPath("$.data").value(true));

            // when & then - 캐릭터가 없는 멤버 검증
            mockMvc.perform(get(BASE_URL + "/exists")
                    .param("memberId", "no-character-member"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));
        }
    }

    @Nested
    @DisplayName("경험치 추가 API 테스트")
    class AddExperienceApiTest {

        @Test
        @DisplayName("경험치 추가 시 200 응답을 반환한다")
        void addExperience_success() throws Exception {
            // given - 경험치 추가 요청 및 Mock 응답 준비
            AddExperienceRequest request = new AddExperienceRequest(100);
            GameCharacterResponse mockResponse = createMockGameCharacterResponse();
            mockResponse = new GameCharacterResponse(
                mockResponse.id(),
                mockResponse.memberId(),
                mockResponse.raceId(),
                2, // 레벨업
                100L, // 경험치 추가
                mockResponse.unspentPoints(),
                mockResponse.strength(),
                mockResponse.willpower(),
                mockResponse.intelligence(),
                mockResponse.wisdom(),
                mockResponse.dexterity(),
                mockResponse.luck(),
                mockResponse.createdAt(),
                mockResponse.updatedAt()
            );
            
            given(gameCharacterService.addExperience(CHARACTER_ID, 100)).willReturn(mockResponse);

            // when & then - 경험치 추가 요청 및 응답 검증
            mockMvc.perform(post(BASE_URL + "/{id}/experience", CHARACTER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.msg").value("경험치 추가 완료"))
                .andExpect(jsonPath("$.data.id").value(CHARACTER_ID))
                .andExpect(jsonPath("$.data.playerLevel").value(2))
                .andExpect(jsonPath("$.data.totalExp").value(100));
        }

        // 예외 처리 테스트는 비즈니스 로직 수준에서 검증됨 (Service 테스트)

        @Test
        @DisplayName("잘못된 경험치 값으로 요청 시 400 응답을 반환한다")
        void addExperience_invalidExperience_badRequest() throws Exception {
            // given - 잘못된 경험치 값 요청
            String invalidJson = "{\"experience\": \"invalid\"}";

            // when & then - 잘못된 요청에 대한 400 응답 검증
            mockMvc.perform(post(BASE_URL + "/{id}/experience", CHARACTER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                .andExpect(status().isBadRequest());
        }
    }
}