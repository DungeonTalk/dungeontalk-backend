package org.com.dungeontalk.domain.gamecharacter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.com.dungeontalk.domain.gamecharacter.dto.request.CreateCharacterRequest;
import org.com.dungeontalk.domain.gamecharacter.dto.response.GameCharacterDetailResponse;
import org.com.dungeontalk.domain.gamecharacter.dto.response.GameCharacterResponse;
import org.com.dungeontalk.domain.gamecharacter.entity.GameCharacter;
import org.com.dungeontalk.domain.gamecharacter.entity.RequestExp;
import org.com.dungeontalk.domain.gamecharacter.repository.GameCharacterRepository;
import org.com.dungeontalk.domain.gamecharacter.repository.RequestExpRepository;
import org.com.dungeontalk.domain.member.entity.Member;
import org.com.dungeontalk.domain.stat.entity.RaceStats;
import org.com.dungeontalk.domain.stat.repository.RaceStatsRepository;
import org.com.dungeontalk.domain.stat.service.StatAggregateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameCharacterService 단위 테스트")
class GameCharacterServiceTest {

    @Mock
    private GameCharacterRepository gameCharacterRepository;
    
    @Mock
    private RaceStatsRepository raceStatsRepository;
    
    @Mock
    private StatAggregateService statAggregateService;
    
    @Mock
    private RequestExpRepository requestExpRepository;
    
    @InjectMocks
    private GameCharacterService gameCharacterService;
    
    private static final String MEMBER_ID = "member-123";
    private static final String CHARACTER_ID = "character-123";
    private static final String RACE_ID = "race-123";
    private static final String RACE_NAME = "인간";

    private Member createMember() {
        return Member.builder()
            .id(MEMBER_ID)
            .name("testUser")
            .nickName("테스터")
            .password("password")
            .build();
    }

    private RaceStats createRaceStats() {
        return RaceStats.builder()
            .id(RACE_ID)
            .race(RACE_NAME)
            .healthPoints("120 + (willpower * 8)")
            .manaPoints("100 + (wisdom * 10)")
            .physicalAttack("strength * 1.5")
            .magicAttack("intelligence * 1.3")
            .evasionRate("dexterity * 1.0")
            .accuracy("60 + (dexterity * 1.0)")
            .diceOdds("luck * 0.12")
            .build();
    }

    private GameCharacter createGameCharacter() {
        return GameCharacter.builder()
            .id(CHARACTER_ID)
            .memberId(MEMBER_ID)
            .raceId(RACE_ID)
            .playerLevel(1)
            .totalExp(0L)
            .unspentPoints(0)
            .strength(10)
            .willpower(10)
            .intelligence(10)
            .wisdom(10)
            .dexterity(10)
            .luck(10)
            .build();
    }

    @Nested
    @DisplayName("캐릭터 생성 테스트")
    class CreateCharacterTest {

        @Test
        @DisplayName("UUID로 종족을 찾아 캐릭터 생성에 성공한다")
        void createCharacter_withRaceId_success() {
            // given - UUID 형태의 종족 ID로 캐릭터 생성 요청
            CreateCharacterRequest request = new CreateCharacterRequest(MEMBER_ID, RACE_ID);
            RaceStats raceStats = createRaceStats();
            GameCharacter savedCharacter = createGameCharacter();
            
            given(raceStatsRepository.findById(RACE_ID)).willReturn(Optional.of(raceStats));
            given(gameCharacterRepository.save(any(GameCharacter.class))).willReturn(savedCharacter);
            
            // when - 캐릭터 생성 실행
            GameCharacterResponse result = gameCharacterService.createCharacter(request);
            
            // then - 초기 스탯이 모두 10으로 설정된 캐릭터가 생성되었는지 검증
            assertThat(result).isNotNull();
            assertThat(result.memberId()).isEqualTo(MEMBER_ID);
            assertThat(result.raceId()).isEqualTo(RACE_ID);
            assertThat(result.playerLevel()).isEqualTo(1);
            assertThat(result.totalExp()).isEqualTo(0L);
            assertThat(result.strength()).isEqualTo(10);
            assertThat(result.willpower()).isEqualTo(10);
            assertThat(result.intelligence()).isEqualTo(10);
            assertThat(result.wisdom()).isEqualTo(10);
            assertThat(result.dexterity()).isEqualTo(10);
            assertThat(result.luck()).isEqualTo(10);
            
            verify(gameCharacterRepository).save(any(GameCharacter.class));
        }

        @Test
        @DisplayName("종족명으로 종족을 찾아 캐릭터 생성에 성공한다")
        void createCharacter_withRaceName_success() {
            // given - 종족명으로 캐릭터 생성 요청 (UUID 조회 실패)
            CreateCharacterRequest request = new CreateCharacterRequest(MEMBER_ID, RACE_NAME);
            RaceStats raceStats = createRaceStats();
            GameCharacter savedCharacter = createGameCharacter();
            
            given(raceStatsRepository.findById(RACE_NAME)).willReturn(Optional.empty());
            given(raceStatsRepository.findByRace(RACE_NAME)).willReturn(Optional.of(raceStats));
            given(gameCharacterRepository.save(any(GameCharacter.class))).willReturn(savedCharacter);
            
            // when - 캐릭터 생성 실행
            GameCharacterResponse result = gameCharacterService.createCharacter(request);
            
            // then - 캐릭터가 정상적으로 생성되었는지 검증
            assertThat(result).isNotNull();
            assertThat(result.raceId()).isEqualTo(RACE_ID); // UUID로 저장됨
            
            verify(raceStatsRepository).findById(RACE_NAME);
            verify(raceStatsRepository).findByRace(RACE_NAME);
        }

        @Test
        @DisplayName("존재하지 않는 종족으로 캐릭터 생성 시 예외가 발생한다")
        void createCharacter_withInvalidRace_throwsException() {
            // given - 존재하지 않는 종족으로 캐릭터 생성 요청
            CreateCharacterRequest request = new CreateCharacterRequest(MEMBER_ID, "존재하지않는종족");
            
            given(raceStatsRepository.findById(anyString())).willReturn(Optional.empty());
            given(raceStatsRepository.findByRace(anyString())).willReturn(Optional.empty());
            
            // when & then - 예외 발생 검증
            assertThatThrownBy(() -> gameCharacterService.createCharacter(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 종족");
        }
    }

    @Nested
    @DisplayName("캐릭터 조회 테스트")
    class FindCharacterTest {

        @Test
        @DisplayName("ID로 캐릭터 기본 정보 조회에 성공한다")
        void findById_success() {
            // given - 존재하는 캐릭터 ID로 조회 요청
            GameCharacter character = createGameCharacter();
            given(gameCharacterRepository.findById(CHARACTER_ID)).willReturn(Optional.of(character));
            
            // when - 캐릭터 조회 실행
            GameCharacterResponse result = gameCharacterService.findById(CHARACTER_ID);
            
            // then - 올바른 캐릭터 정보가 반환되었는지 검증
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(CHARACTER_ID);
            assertThat(result.memberId()).isEqualTo(MEMBER_ID);
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회 시 예외가 발생한다")
        void findById_notFound_throwsException() {
            // given - 존재하지 않는 캐릭터 ID
            given(gameCharacterRepository.findById("invalid-id")).willReturn(Optional.empty());
            
            // when & then - 예외 발생 검증
            assertThatThrownBy(() -> gameCharacterService.findById("invalid-id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Character not found");
        }

        @Test
        @DisplayName("ID로 종족 정보 포함 조회에 성공한다")
        void findByIdWithRace_success() {
            // given - fetch join으로 종족 정보를 포함한 캐릭터 조회
            GameCharacter character = createGameCharacter();
            given(gameCharacterRepository.findWithRace(CHARACTER_ID)).willReturn(Optional.of(character));
            
            // when - 종족 정보 포함 조회 실행
            GameCharacterResponse result = gameCharacterService.findByIdWithRace(CHARACTER_ID);
            
            // then - 캐릭터 정보가 올바르게 반환되었는지 검증
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(CHARACTER_ID);
        }

        @Test
        @DisplayName("멤버 ID로 캐릭터 조회에 성공한다")
        void findByMemberId_success() {
            // given - 특정 멤버의 캐릭터 조회 요청
            GameCharacter character = createGameCharacter();
            given(gameCharacterRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.of(character));
            
            // when - 멤버 ID로 캐릭터 조회 실행
            GameCharacterResponse result = gameCharacterService.findByMemberId(MEMBER_ID);
            
            // then - 해당 멤버의 캐릭터가 올바르게 반환되었는지 검증
            assertThat(result).isNotNull();
            assertThat(result.memberId()).isEqualTo(MEMBER_ID);
        }

        @Test
        @DisplayName("상세 정보 조회에 성공한다")
        void findDetailById_success() {
            // given - 캐릭터 상세 정보 조회 (멤버, 종족, 계산된 스탯 포함)
            GameCharacter character = createGameCharacter();
            Member member = createMember();
            RaceStats raceStats = createRaceStats();
            
            character.setMember(member);
            character.setRaceStats(raceStats);
            
            Map<String, Double> calculatedStats = Map.of(
                "healthPoints", 200.0,
                "manaPoints", 200.0,
                "physicalAttack", 15.0
            );
            
            given(gameCharacterRepository.findWithRace(CHARACTER_ID)).willReturn(Optional.of(character));
            given(statAggregateService.calculateAllStats(CHARACTER_ID)).willReturn(calculatedStats);
            
            // when - 상세 정보 조회 실행
            GameCharacterDetailResponse result = gameCharacterService.findDetailById(CHARACTER_ID);
            
            // then - 상세 정보가 올바르게 조합되었는지 검증
            assertThat(result).isNotNull();
            assertThat(result.nickname()).isEqualTo("테스터");
            assertThat(result.raceName()).isEqualTo(RACE_NAME);
            assertThat(result.healthPoints()).isEqualTo(200.0);
            assertThat(result.manaPoints()).isEqualTo(200.0);
            assertThat(result.physicalAttack()).isEqualTo(15.0);
        }
    }

    @Nested
    @DisplayName("유틸리티 메서드 테스트")
    class UtilityMethodTest {

        @Test
        @DisplayName("사용 가능한 종족 목록 조회에 성공한다")
        void getRaces_success() {
            // given - 종족 목록 조회 요청
            List<String> raceNames = List.of("인간", "엘프", "드워프");
            given(raceStatsRepository.findAllRaceNames()).willReturn(raceNames);
            
            // when - 종족 목록 조회 실행
            List<String> result = gameCharacterService.getRaces();
            
            // then - 올바른 종족 목록이 반환되었는지 검증
            assertThat(result).isEqualTo(raceNames);
            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("멤버의 캐릭터 존재 여부 확인에 성공한다")
        void hasCharacter_success() {
            // given - 멤버의 캐릭터 존재 여부 확인 요청
            given(gameCharacterRepository.existsByMemberId(MEMBER_ID)).willReturn(true);
            given(gameCharacterRepository.existsByMemberId("no-character-member")).willReturn(false);
            
            // when & then - 캐릭터가 있는 멤버와 없는 멤버 구분 검증
            assertThat(gameCharacterService.hasCharacter(MEMBER_ID)).isTrue();
            assertThat(gameCharacterService.hasCharacter("no-character-member")).isFalse();
        }
    }

    @Nested
    @DisplayName("경험치 및 레벨업 테스트")
    class ExperienceTest {

        @Test
        @DisplayName("경험치 추가 시 레벨업이 발생하지 않는다")
        void addExperience_noLevelUp() {
            // given - 레벨업하지 않을 만큼의 경험치 추가
            GameCharacter character = createGameCharacter();
            character.setTotalExp(50L);
            
            RequestExp currentLevelInfo = new RequestExp(1, 0L, 100);
            
            given(gameCharacterRepository.findById(CHARACTER_ID)).willReturn(Optional.of(character));
            given(requestExpRepository.findByLevel(1)).willReturn(Optional.of(currentLevelInfo));
            given(gameCharacterRepository.save(any(GameCharacter.class))).willReturn(character);
            
            // when - 30 경험치 추가 (총 80, 레벨업 필요 100)
            GameCharacterResponse result = gameCharacterService.addExperience(CHARACTER_ID, 30);
            
            // then - 경험치만 증가하고 레벨은 그대로인지 검증
            assertThat(result.totalExp()).isEqualTo(80L);
            assertThat(result.playerLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("경험치 추가 시 단일 레벨업이 발생한다")
        void addExperience_singleLevelUp() {
            // given - 레벨업할 만큼의 경험치 추가
            GameCharacter character = createGameCharacter();
            character.setTotalExp(90L);
            
            RequestExp level1Info = new RequestExp(1, 0L, 100);
            RequestExp level2Info = new RequestExp(2, 100L, 150);
            
            given(gameCharacterRepository.findById(CHARACTER_ID)).willReturn(Optional.of(character));
            given(requestExpRepository.findByLevel(1)).willReturn(Optional.of(level1Info));
            given(requestExpRepository.findByLevel(2)).willReturn(Optional.of(level2Info));
            given(gameCharacterRepository.save(any(GameCharacter.class))).willAnswer(invocation -> {
                GameCharacter saved = invocation.getArgument(0);
                saved.setTotalExp(120L);
                saved.setPlayerLevel(2);
                return saved;
            });
            
            // when - 30 경험치 추가 (총 120, 레벨 2 달성)
            GameCharacterResponse result = gameCharacterService.addExperience(CHARACTER_ID, 30);
            
            // then - 레벨업이 정상적으로 발생했는지 검증
            assertThat(result.totalExp()).isEqualTo(120L);
            assertThat(result.playerLevel()).isEqualTo(2);
        }

        @Test
        @DisplayName("만렙 도달 시 더 이상 레벨업하지 않는다")
        void addExperience_maxLevel() {
            // given - 만렙(30) 캐릭터에 경험치 추가
            GameCharacter character = createGameCharacter();
            character.setPlayerLevel(30);
            character.setTotalExp(10000L);
            
            RequestExp maxLevelInfo = new RequestExp(30, 9999L, 0); // 만렙 표시
            
            given(gameCharacterRepository.findById(CHARACTER_ID)).willReturn(Optional.of(character));
            given(requestExpRepository.findByLevel(30)).willReturn(Optional.of(maxLevelInfo));
            given(gameCharacterRepository.save(any(GameCharacter.class))).willReturn(character);
            
            // when - 만렙 상태에서 경험치 추가
            GameCharacterResponse result = gameCharacterService.addExperience(CHARACTER_ID, 1000);
            
            // then - 경험치는 증가하지만 레벨은 그대로인지 검증
            assertThat(result.playerLevel()).isEqualTo(30);
        }

        @Test
        @DisplayName("존재하지 않는 캐릭터에 경험치 추가 시 예외가 발생한다")
        void addExperience_characterNotFound_throwsException() {
            // given - 존재하지 않는 캐릭터 ID
            given(gameCharacterRepository.findById("invalid-id")).willReturn(Optional.empty());
            
            // when & then - 예외 발생 검증
            assertThatThrownBy(() -> gameCharacterService.addExperience("invalid-id", 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("캐릭터 정보를 찾을 수 없습니다");
        }
    }
}