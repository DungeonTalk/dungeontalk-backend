package org.com.dungeontalk.domain.stat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.com.dungeontalk.domain.gamecharacter.entity.GameCharacter;
import org.com.dungeontalk.domain.gamecharacter.repository.GameCharacterRepository;
import org.com.dungeontalk.domain.member.entity.Member;
import org.com.dungeontalk.domain.stat.entity.RaceStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatAggregateService 단위 테스트")
class StatAggregateServiceTest {

    @Mock
    private GameCharacterRepository gameCharacterRepository;
    
    @Mock
    private StatCalculatorService statCalculatorService;
    
    @InjectMocks
    private StatAggregateService statAggregateService;
    
    private static final String CHARACTER_ID = "character-123";
    private static final String MEMBER_ID = "member-123";
    private static final String RACE_ID = "race-123";

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
            .race("인간")
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
        GameCharacter character = GameCharacter.builder()
            .id(CHARACTER_ID)
            .memberId(MEMBER_ID)
            .raceId(RACE_ID)
            .playerLevel(2)
            .totalExp(150L)
            .unspentPoints(3)
            .strength(12)
            .willpower(14)
            .intelligence(16)
            .wisdom(13)
            .dexterity(11)
            .luck(9)
            .build();
            
        // 연관 엔티티 설정
        character.setMember(createMember());
        character.setRaceStats(createRaceStats());
        
        return character;
    }

    @Nested
    @DisplayName("전체 스탯 계산 테스트")
    class CalculateAllStatsTest {

        @Test
        @DisplayName("모든 스탯 계산에 성공한다")
        void calculateAllStats_success() {
            // given - 캐릭터와 계산된 스탯 값들 준비
            GameCharacter character = createGameCharacter();
            
            given(gameCharacterRepository.findWithRace(CHARACTER_ID))
                .willReturn(Optional.of(character));
            
            // StatCalculatorService의 각 공식별 계산 결과 Mock 설정
            given(statCalculatorService.calculate("120 + (willpower * 8)", character.toVariableMap()))
                .willReturn(232.0); // 120 + (14 * 8)
            given(statCalculatorService.calculate("100 + (wisdom * 10)", character.toVariableMap()))
                .willReturn(230.0); // 100 + (13 * 10)
            given(statCalculatorService.calculate("strength * 1.5", character.toVariableMap()))
                .willReturn(18.0); // 12 * 1.5
            given(statCalculatorService.calculate("intelligence * 1.3", character.toVariableMap()))
                .willReturn(20.8); // 16 * 1.3
            given(statCalculatorService.calculate("dexterity * 1.0", character.toVariableMap()))
                .willReturn(11.0); // 11 * 1.0
            given(statCalculatorService.calculate("60 + (dexterity * 1.0)", character.toVariableMap()))
                .willReturn(71.0); // 60 + 11
            given(statCalculatorService.calculate("luck * 0.12", character.toVariableMap()))
                .willReturn(1.08); // 9 * 0.12
            
            // when - 전체 스탯 계산 실행
            Map<String, Double> result = statAggregateService.calculateAllStats(CHARACTER_ID);
            
            // then - 계산 결과 검증 (스네이크케이스 → 카멜케이스 변환 포함)
            assertThat(result).hasSize(7);
            assertThat(result.get("healthPoints")).isEqualTo(232.0);
            assertThat(result.get("manaPoints")).isEqualTo(230.0);
            assertThat(result.get("physicalAttack")).isEqualTo(18.0);
            assertThat(result.get("magicAttack")).isEqualTo(20.8);
            assertThat(result.get("evasionRate")).isEqualTo(11.0);
            assertThat(result.get("accuracy")).isEqualTo(71.0);
            assertThat(result.get("diceOdds")).isEqualTo(1.08);
            
            // Repository와 Calculator 호출 검증
            verify(gameCharacterRepository).findWithRace(CHARACTER_ID);
            verify(statCalculatorService).calculate("120 + (willpower * 8)", character.toVariableMap());
            verify(statCalculatorService).calculate("100 + (wisdom * 10)", character.toVariableMap());
            verify(statCalculatorService).calculate("strength * 1.5", character.toVariableMap());
            verify(statCalculatorService).calculate("intelligence * 1.3", character.toVariableMap());
            verify(statCalculatorService).calculate("dexterity * 1.0", character.toVariableMap());
            verify(statCalculatorService).calculate("60 + (dexterity * 1.0)", character.toVariableMap());
            verify(statCalculatorService).calculate("luck * 0.12", character.toVariableMap());
        }

        @Test
        @DisplayName("빈 공식이 있는 종족의 스탯 계산에 성공한다")
        void calculateAllStats_withEmptyFormulas_success() {
            // given - 일부 공식이 빈 종족 스탯
            GameCharacter character = createGameCharacter();
            RaceStats partialRaceStats = RaceStats.builder()
                .id(RACE_ID)
                .race("부분종족")
                .healthPoints("100 + (willpower * 5)")
                .manaPoints("") // 빈 공식
                .physicalAttack(null) // null 공식
                .magicAttack("intelligence * 1.0")
                .evasionRate("") // 빈 공식
                .accuracy("50")
                .diceOdds(null) // null 공식
                .build();
            
            character.setRaceStats(partialRaceStats);
            
            given(gameCharacterRepository.findWithRace(CHARACTER_ID))
                .willReturn(Optional.of(character));
            
            // 빈 공식이 아닌 것만 계산 Mock 설정
            given(statCalculatorService.calculate("100 + (willpower * 5)", character.toVariableMap()))
                .willReturn(170.0);
            given(statCalculatorService.calculate("intelligence * 1.0", character.toVariableMap()))
                .willReturn(16.0);
            given(statCalculatorService.calculate("50", character.toVariableMap()))
                .willReturn(50.0);
            
            // when - 부분적인 공식을 가진 종족의 스탯 계산
            Map<String, Double> result = statAggregateService.calculateAllStats(CHARACTER_ID);
            
            // then - 빈 공식이 아닌 것들만 결과에 포함되는지 검증
            assertThat(result).hasSize(3);
            assertThat(result.get("healthPoints")).isEqualTo(170.0);
            assertThat(result.get("magicAttack")).isEqualTo(16.0);
            assertThat(result.get("accuracy")).isEqualTo(50.0);
            
            // 빈 공식들은 결과에 포함되지 않음
            assertThat(result).doesNotContainKeys("manaPoints", "physicalAttack", "evasionRate", "diceOdds");
        }

        @Test
        @DisplayName("복잡한 공식의 스탯 계산에 성공한다")
        void calculateAllStats_complexFormulas_success() {
            // given - 복잡한 공식을 가진 종족 스탯
            GameCharacter character = createGameCharacter();
            RaceStats complexRaceStats = RaceStats.builder()
                .id(RACE_ID)
                .race("복합종족")
                .healthPoints("(strength + willpower) * 5 + 50")
                .manaPoints("intelligence * wisdom / 2 + 100")
                .physicalAttack("strength * 1.8 + dexterity * 0.2")
                .magicAttack("(intelligence + wisdom) * 0.75")
                .evasionRate("dexterity * 1.2 + luck * 0.3")
                .accuracy("70 + dexterity * 0.8 + luck * 0.4")
                .diceOdds("luck * 0.15 + intelligence * 0.05")
                .build();
            
            character.setRaceStats(complexRaceStats);
            
            given(gameCharacterRepository.findWithRace(CHARACTER_ID))
                .willReturn(Optional.of(character));
            
            // 복잡한 공식들의 계산 결과 Mock 설정
            given(statCalculatorService.calculate("(strength + willpower) * 5 + 50", character.toVariableMap()))
                .willReturn(180.0); // (12 + 14) * 5 + 50
            given(statCalculatorService.calculate("intelligence * wisdom / 2 + 100", character.toVariableMap()))
                .willReturn(204.0); // 16 * 13 / 2 + 100
            given(statCalculatorService.calculate("strength * 1.8 + dexterity * 0.2", character.toVariableMap()))
                .willReturn(23.8); // 12 * 1.8 + 11 * 0.2
            given(statCalculatorService.calculate("(intelligence + wisdom) * 0.75", character.toVariableMap()))
                .willReturn(21.75); // (16 + 13) * 0.75
            given(statCalculatorService.calculate("dexterity * 1.2 + luck * 0.3", character.toVariableMap()))
                .willReturn(15.9); // 11 * 1.2 + 9 * 0.3
            given(statCalculatorService.calculate("70 + dexterity * 0.8 + luck * 0.4", character.toVariableMap()))
                .willReturn(82.4); // 70 + 11 * 0.8 + 9 * 0.4
            given(statCalculatorService.calculate("luck * 0.15 + intelligence * 0.05", character.toVariableMap()))
                .willReturn(2.15); // 9 * 0.15 + 16 * 0.05
            
            // when - 복잡한 공식들의 스탯 계산
            Map<String, Double> result = statAggregateService.calculateAllStats(CHARACTER_ID);
            
            // then - 복잡한 공식 계산 결과 검증
            assertThat(result).hasSize(7);
            assertThat(result.get("healthPoints")).isEqualTo(180.0);
            assertThat(result.get("manaPoints")).isEqualTo(204.0);
            assertThat(result.get("physicalAttack")).isEqualTo(23.8);
            assertThat(result.get("magicAttack")).isEqualTo(21.75);
            assertThat(result.get("evasionRate")).isEqualTo(15.9);
            assertThat(result.get("accuracy")).isEqualTo(82.4);
            assertThat(result.get("diceOdds")).isEqualTo(2.15);
        }
    }

    @Nested
    @DisplayName("예외 상황 테스트")
    class ExceptionTest {

        @Test
        @DisplayName("존재하지 않는 캐릭터 ID로 계산 시 예외가 발생한다")
        void calculateAllStats_characterNotFound_throwsException() {
            // given - 존재하지 않는 캐릭터 ID
            given(gameCharacterRepository.findWithRace("invalid-id"))
                .willReturn(Optional.empty());
            
            // when & then - 캐릭터 없음 예외 발생 검증
            assertThatThrownBy(() -> statAggregateService.calculateAllStats("invalid-id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("캐릭터 없음: invalid-id");
                
            verify(gameCharacterRepository).findWithRace("invalid-id");
        }

        @Test
        @DisplayName("종족 스탯 정보가 없는 캐릭터로 계산 시 예외가 발생한다")
        void calculateAllStats_noRaceStats_throwsException() {
            // given - 종족 스탯 정보가 없는 캐릭터
            GameCharacter characterWithoutRace = createGameCharacter();
            characterWithoutRace.setRaceStats(null); // 종족 스탯 없음
            
            given(gameCharacterRepository.findWithRace(CHARACTER_ID))
                .willReturn(Optional.of(characterWithoutRace));
            
            // when & then - 종족 스탯 공식 없음 예외 발생 검증
            assertThatThrownBy(() -> statAggregateService.calculateAllStats(CHARACTER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("종족 스탯 공식 없음: " + RACE_ID);
                
            verify(gameCharacterRepository).findWithRace(CHARACTER_ID);
        }

        @Test
        @DisplayName("스탯 계산 중 오류 발생 시 예외를 전파한다")
        void calculateAllStats_calculationError_propagatesException() {
            // given - 계산 중 오류가 발생하는 상황
            GameCharacter character = createGameCharacter();
            
            given(gameCharacterRepository.findWithRace(CHARACTER_ID))
                .willReturn(Optional.of(character));
            
            // StatCalculatorService에서 예외 발생 Mock 설정
            given(statCalculatorService.calculate("120 + (willpower * 8)", character.toVariableMap()))
                .willThrow(new IllegalArgumentException("변수 'willpower'가 누락되었습니다"));
            
            // when & then - 계산 오류 예외 전파 검증
            assertThatThrownBy(() -> statAggregateService.calculateAllStats(CHARACTER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("변수 'willpower'가 누락되었습니다");
                
            verify(gameCharacterRepository).findWithRace(CHARACTER_ID);
            verify(statCalculatorService).calculate("120 + (willpower * 8)", character.toVariableMap());
        }
    }

    @Nested
    @DisplayName("케이스 변환 테스트")
    class CaseConversionTest {

        @Test
        @DisplayName("스네이크케이스가 카멜케이스로 올바르게 변환된다")
        void toCamelCase_conversion_success() {
            // given - 스네이크케이스 컬럼명을 가진 종족 스탯
            GameCharacter character = createGameCharacter();
            RaceStats raceStats = character.getRaceStats();
            
            given(gameCharacterRepository.findWithRace(CHARACTER_ID))
                .willReturn(Optional.of(character));
            
            // 각 스탯별 계산 결과 Mock 설정
            given(statCalculatorService.calculate("120 + (willpower * 8)", character.toVariableMap()))
                .willReturn(232.0);
            given(statCalculatorService.calculate("100 + (wisdom * 10)", character.toVariableMap()))
                .willReturn(230.0);
            given(statCalculatorService.calculate("strength * 1.5", character.toVariableMap()))
                .willReturn(18.0);
            given(statCalculatorService.calculate("intelligence * 1.3", character.toVariableMap()))
                .willReturn(20.8);
            given(statCalculatorService.calculate("dexterity * 1.0", character.toVariableMap()))
                .willReturn(11.0);
            given(statCalculatorService.calculate("60 + (dexterity * 1.0)", character.toVariableMap()))
                .willReturn(71.0);
            given(statCalculatorService.calculate("luck * 0.12", character.toVariableMap()))
                .willReturn(1.08);
            
            // when - 스탯 계산 실행
            Map<String, Double> result = statAggregateService.calculateAllStats(CHARACTER_ID);
            
            // then - 스네이크케이스가 카멜케이스로 변환되었는지 검증
            // health_points → healthPoints
            assertThat(result).containsKey("healthPoints");
            assertThat(result).doesNotContainKey("health_points");
            
            // mana_points → manaPoints  
            assertThat(result).containsKey("manaPoints");
            assertThat(result).doesNotContainKey("mana_points");
            
            // physical_attack → physicalAttack
            assertThat(result).containsKey("physicalAttack");
            assertThat(result).doesNotContainKey("physical_attack");
            
            // magic_attack → magicAttack
            assertThat(result).containsKey("magicAttack");
            assertThat(result).doesNotContainKey("magic_attack");
            
            // evasion_rate → evasionRate
            assertThat(result).containsKey("evasionRate");
            assertThat(result).doesNotContainKey("evasion_rate");
            
            // dice_odds → diceOdds
            assertThat(result).containsKey("diceOdds");
            assertThat(result).doesNotContainKey("dice_odds");
            
            // accuracy는 이미 카멜케이스이므로 그대로
            assertThat(result).containsKey("accuracy");
        }
    }
}