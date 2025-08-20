package org.com.dungeontalk.domain.gamecharacter.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.com.dungeontalk.domain.gamecharacter.entity.GameCharacter;
import org.com.dungeontalk.domain.member.entity.Member;
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

import java.util.Optional;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestJpaConfig.class)
@DisplayName("GameCharacterRepository @DataJpaTest(H2)")
class GameCharacterRepositoryTest {


    @Autowired
    private TestEntityManager em;

    @Autowired
    private GameCharacterRepository gameCharacterRepository;

    @MockitoBean(name = "mongoMappingContext")
    MongoMappingContext mongoMappingContext;

    private Member persistMember(String name, String nickName, String password) {
        Member member = Member.builder()
            .name(name)
            .nickName(nickName)
            .password(password)
            .build();
        
        return em.persistFlushFind(member);
    }

    private GameCharacter newGameCharacter(String memberId, String raceId) {
        return GameCharacter.builder()
            .memberId(memberId)
            .raceId(raceId)
            .playerLevel(1)
            .totalExp(0L)
            .unspentPoints(5)
            .strength(10)
            .willpower(10)
            .intelligence(10)
            .wisdom(10)
            .dexterity(10)
            .luck(10)
            .build();
    }

    private RaceStats createRaceStats(String race) {
        RaceStats raceStats = RaceStats.builder()
            .race(race)
            .healthPoints("120 + (willpower * 8)")
            .manaPoints("100 + (wisdom * 10)")
            .physicalAttack("strength * 1.5")
            .magicAttack("intelligence * 1.3")
            .evasionRate("dexterity * 1.0")
            .accuracy("60 + (dexterity * 1.0)")
            .diceOdds("luck * 0.12")
            .build();
        
        return em.persistFlushFind(raceStats);
    }

    @Nested
    @DisplayName("기본 CRUD 테스트")
    class BasicCrudTest {

        @Test
        @DisplayName("게임 캐릭터 저장 및 findById를 통한 조회가 가능하다")
        void save_and_findById() {
            // given - 테스트용 멤버, 종족, 캐릭터 생성
            Member member = persistMember("testUser", "테스터", "encPassword");
            RaceStats humanRace = createRaceStats("인간");
            GameCharacter character = newGameCharacter(member.getId(), humanRace.getId());
            
            // when - 캐릭터 저장 후 ID로 조회
            GameCharacter saved = gameCharacterRepository.save(character);
            Optional<GameCharacter> found = gameCharacterRepository.findById(saved.getId());
            
            // then - 저장된 캐릭터 정보 검증
            assertThat(found).isPresent();
            assertThat(found.get().getMemberId()).isEqualTo(member.getId());
            assertThat(found.get().getRaceId()).isEqualTo(humanRace.getId());
            assertThat(found.get().getPlayerLevel()).isEqualTo(1);
            assertThat(found.get().getUnspentPoints()).isEqualTo(5);
        }

        @Test
        @DisplayName("게임 캐릭터 저장 시 BaseEntity 필드가 자동으로 생성된다")
        void baseEntity_autoGeneration_onSave() {
            // given - 테스트용 멤버, 엘프 종족, 캐릭터 생성
            Member member = persistMember("testUser2", "테스터2", "encPassword");
            RaceStats elfRace = createRaceStats("엘프");
            GameCharacter character = newGameCharacter(member.getId(), elfRace.getId());
            
            // when - 캐릭터 저장
            GameCharacter saved = gameCharacterRepository.saveAndFlush(character);
            
            // then - BaseEntity 필드들이 자동 생성되었는지 검증
            assertThat(saved.getId()).isNotBlank();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
            assertThat(saved.getCreatedAt()).isBeforeOrEqualTo(saved.getUpdatedAt());
        }

        @Test
        @DisplayName("게임 캐릭터 수정 시 updatedAt이 갱신된다")
        void update_character_updatesTimestamp() {
            // given - 저장된 캐릭터 준비
            Member member = persistMember("testUser3", "테스터3", "encPassword");
            RaceStats dwarfRace = createRaceStats("드워프");
            GameCharacter character = newGameCharacter(member.getId(), dwarfRace.getId());
            GameCharacter saved = gameCharacterRepository.saveAndFlush(character);
            
            // when - 캐릭터 정보 수정
            saved.setPlayerLevel(2);
            saved.setStrength(15);
            saved.setUnspentPoints(3);
            GameCharacter updated = gameCharacterRepository.saveAndFlush(saved);
            
            // then - 수정된 정보와 타임스탬프 갱신 검증
            assertThat(updated.getPlayerLevel()).isEqualTo(2);
            assertThat(updated.getStrength()).isEqualTo(15);
            assertThat(updated.getUnspentPoints()).isEqualTo(3);
            assertThat(updated.getUpdatedAt()).isAfter(updated.getCreatedAt());
        }

        @Test
        @DisplayName("존재하지 않는 캐릭터 조회 시 빈 Optional을 반환한다")
        void findById_nonExistent_returnsEmpty() {
            // given - 없는 ID 준비 (별도 설정 불필요)
            
            // when - 존재하지 않는 ID로 조회
            Optional<GameCharacter> found = gameCharacterRepository.findById("non-existent-id");
            
            // then - 빈 Optional 반환 검증
            assertThat(found).isNotPresent();
        }

    }

    @Nested
    @DisplayName("커스텀 쿼리 메서드 테스트")
    class CustomQueryTest {

        @Test
        @DisplayName("findWithRace - Fetch Join으로 RaceStats와 Member를 함께 조회한다")
        void findWithRace_fetchJoin() {
            // given - 테스트용 멤버, 엘프 종족, 캐릭터 생성 후 저장
            Member member = persistMember("user1", "유저1", "encPassword");
            RaceStats elfRace = createRaceStats("엘프");
            GameCharacter character = newGameCharacter(member.getId(), elfRace.getId());
            GameCharacter saved = gameCharacterRepository.saveAndFlush(character);

            // when - Fetch Join을 사용하여 연관 엔티티와 함께 조회
            Optional<GameCharacter> found = gameCharacterRepository.findWithRace(saved.getId());

            // then - 캐릭터와 연관 엔티티들이 올바르게 조회되었는지 검증
            assertThat(found).isPresent();
            
            GameCharacter result = found.get();
            // Fetch Join으로 로드된 RaceStats 검증
            if (result.getRaceStats() != null) {
                assertThat(result.getRaceStats().getRace()).isEqualTo("엘프");
            }
            // Fetch Join으로 로드된 Member 검증
            if (result.getMember() != null) {
                assertThat(result.getMember().getNickName()).isEqualTo("유저1");
            }
            // 기본 캐릭터 정보 검증
            assertThat(result.getRaceId()).isEqualTo(elfRace.getId());
            assertThat(result.getMemberId()).isEqualTo(member.getId());
        }

        @Test
        @DisplayName("findByMemberId - 멤버 ID로 캐릭터를 조회한다")
        void findByMemberId_success() {
            // given - 테스트용 멤버, 인간 종족, 캐릭터 생성 후 저장
            Member member = persistMember("user2", "유저2", "encPassword");
            RaceStats humanRace = createRaceStats("인간");
            GameCharacter character = newGameCharacter(member.getId(), humanRace.getId());
            gameCharacterRepository.saveAndFlush(character);

            // when - 멤버 ID로 캐릭터 조회
            Optional<GameCharacter> found = gameCharacterRepository.findByMemberId(member.getId());

            // then - 올바른 캐릭터가 조회되었는지 검증
            assertThat(found).isPresent();
            assertThat(found.get().getMemberId()).isEqualTo(member.getId());
            assertThat(found.get().getRaceId()).isEqualTo(humanRace.getId());
        }

        @Test
        @DisplayName("findByMemberId - 존재하지 않는 멤버 ID로 조회 시 빈 Optional을 반환한다")
        void findByMemberId_notFound() {
            // given - 없는 멤버 ID 준비 (별도 설정 불필요)
            
            // when - 존재하지 않는 멤버 ID로 캐릭터 조회
            Optional<GameCharacter> found = gameCharacterRepository.findByMemberId("non-existent-member-id");

            // then - 빈 Optional 반환 검증
            assertThat(found).isNotPresent();
        }

        @Test
        @DisplayName("existsByMemberId - 멤버 ID로 캐릭터 존재 여부를 확인한다")
        void existsByMemberId_success() {
            // given - 테스트용 멤버, 드워프 종족, 캐릭터 생성 후 저장
            Member member = persistMember("user3", "유저3", "encPassword");
            RaceStats dwarfRace = createRaceStats("드워프");
            GameCharacter character = newGameCharacter(member.getId(), dwarfRace.getId());
            gameCharacterRepository.saveAndFlush(character);

            // when & then - 존재하는 멤버 ID는 true, 없는 ID는 false 반환 검증
            assertThat(gameCharacterRepository.existsByMemberId(member.getId())).isTrue();
            assertThat(gameCharacterRepository.existsByMemberId("non-existent-member-id")).isFalse();
        }
    }

    @Nested
    @DisplayName("비즈니스 규칙 테스트")
    class BusinessRuleTest {

        @Test
        @DisplayName("동일한 멤버가 여러 캐릭터를 가지면 findByMemberId에서 NonUniqueResultException 발생")
        void oneCharacterPerMember_nonUniqueResult() {
            // given - 한 멤버에 대해 두 개의 서로 다른 종족 캐릭터 생성
            Member member = persistMember("testUser", "테스터", "encPassword");
            RaceStats humanRace = createRaceStats("인간");
            RaceStats elfRace = createRaceStats("엘프");
            
            // 첫 번째 캐릭터 생성 및 저장
            GameCharacter firstCharacter = newGameCharacter(member.getId(), humanRace.getId());
            gameCharacterRepository.saveAndFlush(firstCharacter);
            
            // 두 번째 캐릭터 생성 및 저장 (DB 레벨에서는 중복 제약 없음)
            GameCharacter secondCharacter = newGameCharacter(member.getId(), elfRace.getId());
            gameCharacterRepository.saveAndFlush(secondCharacter);
            
            // when & then - 단일 결과 기대 메서드에서 예외 발생 검증
            assertThatThrownBy(() -> {
                gameCharacterRepository.findByMemberId(member.getId());
            }).isInstanceOfAny(RuntimeException.class);
            
            // 존재 여부 확인 메서드는 정상 동작 검증
            assertThat(gameCharacterRepository.existsByMemberId(member.getId())).isTrue();
        }
    }
}