package org.com.dungeontalk.domain.stat.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.com.dungeontalk.domain.stat.entity.RaceStats;
import org.com.dungeontalk.global.support.TestJpaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestJpaConfig.class)
@DisplayName("RaceStatsRepository @DataJpaTest(H2)")
class RaceStatsRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private RaceStatsRepository raceStatsRepository;

    @MockitoBean(name = "mongoMappingContext")
    MongoMappingContext mongoMappingContext;

    private RaceStats createRaceStats(String race, String healthFormula, String manaFormula) {
        return RaceStats.builder()
            .race(race)
            .healthPoints(healthFormula)
            .manaPoints(manaFormula)
            .physicalAttack("strength * 1.5")
            .magicAttack("intelligence * 1.3")
            .evasionRate("dexterity * 1.0")
            .accuracy("60 + (dexterity * 1.0)")
            .diceOdds("luck * 0.12")
            .build();
    }

    private RaceStats persistRaceStats(String race, String healthFormula, String manaFormula) {
        RaceStats raceStats = createRaceStats(race, healthFormula, manaFormula);
        return em.persistFlushFind(raceStats);
    }

    @Nested
    @DisplayName("기본 CRUD 테스트")
    class BasicCrudTest {

        @Test
        @DisplayName("종족 스탯 저장 및 findById를 통한 조회가 가능하다")
        void save_and_findById() {
            // given - 테스트용 종족 스탯 생성
            RaceStats humanRace = createRaceStats("인간", "120 + (willpower * 8)", "100 + (wisdom * 10)");
            
            // when - 종족 스탯 저장 후 ID로 조회
            RaceStats saved = raceStatsRepository.save(humanRace);
            Optional<RaceStats> found = raceStatsRepository.findById(saved.getId());
            
            // then - 저장된 종족 스탯 정보 검증
            assertThat(found).isPresent();
            assertThat(found.get().getRace()).isEqualTo("인간");
            assertThat(found.get().getHealthPoints()).isEqualTo("120 + (willpower * 8)");
            assertThat(found.get().getManaPoints()).isEqualTo("100 + (wisdom * 10)");
            assertThat(found.get().getPhysicalAttack()).isEqualTo("strength * 1.5");
            assertThat(found.get().getMagicAttack()).isEqualTo("intelligence * 1.3");
        }

        @Test
        @DisplayName("종족 스탯 저장 시 BaseEntity 필드가 자동으로 생성된다")
        void baseEntity_autoGeneration_onSave() {
            // given - 테스트용 엘프 종족 스탯 생성
            RaceStats elfRace = createRaceStats("엘프", "100 + (willpower * 6)", "150 + (wisdom * 12)");
            
            // when - 종족 스탯 저장
            RaceStats saved = raceStatsRepository.saveAndFlush(elfRace);
            
            // then - BaseEntity 필드들이 자동 생성되었는지 검증
            assertThat(saved.getId()).isNotBlank();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
            assertThat(saved.getCreatedAt()).isBeforeOrEqualTo(saved.getUpdatedAt());
        }

        @Test
        @DisplayName("종족 스탯 수정 시 updatedAt이 갱신된다")
        void update_raceStats_updatesTimestamp() {
            // given - 저장된 종족 스탯 준비
            RaceStats dwarfRace = createRaceStats("드워프", "150 + (willpower * 10)", "80 + (wisdom * 8)");
            RaceStats saved = raceStatsRepository.saveAndFlush(dwarfRace);
            
            // when - 종족 스탯 수정
            saved.setHealthPoints("160 + (willpower * 12)");
            saved.setPhysicalAttack("strength * 2.0");
            RaceStats updated = raceStatsRepository.saveAndFlush(saved);
            
            // then - 수정된 정보와 타임스탬프 갱신 검증
            assertThat(updated.getHealthPoints()).isEqualTo("160 + (willpower * 12)");
            assertThat(updated.getPhysicalAttack()).isEqualTo("strength * 2.0");
            assertThat(updated.getUpdatedAt()).isAfter(updated.getCreatedAt());
        }

        @Test
        @DisplayName("존재하지 않는 종족 스탯 조회 시 빈 Optional을 반환한다")
        void findById_nonExistent_returnsEmpty() {
            // given - 없는 ID 준비 (별도 설정 불필요)
            
            // when - 존재하지 않는 ID로 조회
            Optional<RaceStats> found = raceStatsRepository.findById("non-existent-id");
            
            // then - 빈 Optional 반환 검증
            assertThat(found).isNotPresent();
        }
    }

    @Nested
    @DisplayName("커스텀 쿼리 메서드 테스트")
    class CustomQueryTest {

        @Test
        @DisplayName("findAllRaceNames - 모든 종족명만 조회한다")
        void findAllRaceNames_success() {
            // given - 여러 종족 스탯 데이터 생성 및 저장
            persistRaceStats("인간", "120 + (willpower * 8)", "100 + (wisdom * 10)");
            persistRaceStats("엘프", "100 + (willpower * 6)", "150 + (wisdom * 12)");
            persistRaceStats("드워프", "150 + (willpower * 10)", "80 + (wisdom * 8)");
            
            // when - 모든 종족명 조회
            List<String> raceNames = raceStatsRepository.findAllRaceNames();
            
            // then - 종족명만 올바르게 조회되었는지 검증
            assertThat(raceNames).hasSize(3);
            assertThat(raceNames).containsExactlyInAnyOrder("인간", "엘프", "드워프");
        }

        @Test
        @DisplayName("findAllRaceNames - 데이터가 없을 때 빈 리스트를 반환한다")
        void findAllRaceNames_empty() {
            // given - 데이터 없는 상태 (별도 설정 불필요)
            
            // when - 종족명 조회
            List<String> raceNames = raceStatsRepository.findAllRaceNames();
            
            // then - 빈 리스트 반환 검증
            assertThat(raceNames).isEmpty();
        }

        @Test
        @DisplayName("findByRace - 종족명으로 종족 스탯 조회에 성공한다")
        void findByRace_success() {
            // given - 특정 종족 스탯 데이터 생성 및 저장
            RaceStats humanRace = persistRaceStats("인간", "120 + (willpower * 8)", "100 + (wisdom * 10)");
            persistRaceStats("엘프", "100 + (willpower * 6)", "150 + (wisdom * 12)");
            
            // when - 종족명으로 특정 종족 스탯 조회
            Optional<RaceStats> found = raceStatsRepository.findByRace("인간");
            
            // then - 올바른 종족 스탯이 조회되었는지 검증
            assertThat(found).isPresent();
            assertThat(found.get().getRace()).isEqualTo("인간");
            assertThat(found.get().getHealthPoints()).isEqualTo("120 + (willpower * 8)");
            assertThat(found.get().getManaPoints()).isEqualTo("100 + (wisdom * 10)");
            assertThat(found.get().getId()).isEqualTo(humanRace.getId());
        }

        @Test
        @DisplayName("findByRace - 존재하지 않는 종족명으로 조회 시 빈 Optional을 반환한다")
        void findByRace_notFound() {
            // given - 일부 종족 데이터만 저장
            persistRaceStats("인간", "120 + (willpower * 8)", "100 + (wisdom * 10)");
            
            // when - 존재하지 않는 종족명으로 조회
            Optional<RaceStats> found = raceStatsRepository.findByRace("오크");
            
            // then - 빈 Optional 반환 검증
            assertThat(found).isNotPresent();
        }

        @Test
        @DisplayName("findByRace - 동일한 종족명이 여러 개 있을 경우 NonUniqueResultException 발생")
        void findByRace_duplicateRaceNames_throwsException() {
            // given - 동일한 종족명의 데이터 여러 개 생성 (실제로는 DB 제약으로 방지되어야 하지만 테스트용)
            persistRaceStats("인간", "120 + (willpower * 8)", "100 + (wisdom * 10)");
            persistRaceStats("인간", "130 + (willpower * 9)", "110 + (wisdom * 11)"); // 중복 데이터
            
            // when & then - 중복 데이터로 인한 예외 발생 검증
            assertThatThrownBy(() -> raceStatsRepository.findByRace("인간"))
                .isInstanceOfAny(
                    org.springframework.dao.IncorrectResultSizeDataAccessException.class,
                    org.hibernate.NonUniqueResultException.class
                );
        }
    }

    @Nested
    @DisplayName("비즈니스 규칙 테스트")
    class BusinessRuleTest {

        @Test
        @DisplayName("종족별 스탯 공식이 올바르게 저장된다")
        void statFormulas_savedCorrectly() {
            // given - 다양한 스탯 공식을 가진 종족 생성
            RaceStats complexRace = RaceStats.builder()
                .race("복합종족")
                .healthPoints("(strength + willpower) * 5 + 50")
                .manaPoints("intelligence * wisdom / 2 + 100")
                .physicalAttack("strength * 1.8 + dexterity * 0.2")
                .magicAttack("(intelligence + wisdom) * 0.75")
                .evasionRate("dexterity * 1.2 + luck * 0.3")
                .accuracy("70 + dexterity * 0.8 + luck * 0.4")
                .diceOdds("luck * 0.15 + intelligence * 0.05")
                .build();
            
            // when - 복잡한 공식을 가진 종족 저장 후 조회
            RaceStats saved = raceStatsRepository.saveAndFlush(complexRace);
            Optional<RaceStats> found = raceStatsRepository.findById(saved.getId());
            
            // then - 모든 공식이 정확히 저장되었는지 검증
            assertThat(found).isPresent();
            RaceStats result = found.get();
            assertThat(result.getHealthPoints()).isEqualTo("(strength + willpower) * 5 + 50");
            assertThat(result.getManaPoints()).isEqualTo("intelligence * wisdom / 2 + 100");
            assertThat(result.getPhysicalAttack()).isEqualTo("strength * 1.8 + dexterity * 0.2");
            assertThat(result.getMagicAttack()).isEqualTo("(intelligence + wisdom) * 0.75");
            assertThat(result.getEvasionRate()).isEqualTo("dexterity * 1.2 + luck * 0.3");
            assertThat(result.getAccuracy()).isEqualTo("70 + dexterity * 0.8 + luck * 0.4");
            assertThat(result.getDiceOdds()).isEqualTo("luck * 0.15 + intelligence * 0.05");
        }

    }
}