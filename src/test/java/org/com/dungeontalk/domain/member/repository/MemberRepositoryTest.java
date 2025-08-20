package org.com.dungeontalk.domain.member.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import org.com.dungeontalk.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
@DisplayName("MemberRepository @DataJpaTest(H2)")
class MemberRepositoryTest {

    @Autowired
    MemberRepository memberRepository;

    @MockitoBean(name = "mongoMappingContext")
    MongoMappingContext mongoMappingContext;

    private Member newMember(String name, String nickName) {
        return Member.builder()
            .name(name)
            .nickName(nickName)
            .password("enc-pw")   // not null 컬럼
            .build();
    }

    @Nested
    @DisplayName("기본 CRUD/조회 메서드")
    class BasicTest {

        @Test
        @DisplayName("회원 저장 및 findById 통한 회원 조회 가능 여부 확인한다.")
        void save_and_findById() {
            // given
            Member saved = memberRepository.save(newMember("alice", "앨리스"));

            // when
            Optional<Member> found = memberRepository.findById(saved.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("alice");
            assertThat(found.get().getNickName()).isEqualTo("앨리스");
        }

        @Test
        @DisplayName("회원명과 회원의 닉네임을 조회한다.")
        void findByName_and_findByNickName() {
            // given
            memberRepository.save(newMember("bob", "밥"));
            memberRepository.save(newMember("carol", "캐롤"));

            // when
            Optional<Member> byName = memberRepository.findByName("bob");
            Optional<Member> byNick = memberRepository.findByNickName("캐롤");

            // then
            assertThat(byName).isPresent();
            assertThat(byName.get().getNickName()).isEqualTo("밥");

            assertThat(byNick).isPresent();
            assertThat(byNick.get().getName()).isEqualTo("carol");
        }

        @Test
        @DisplayName("findByIdIn - 여러 ID로 일괄 조회한다.")
        void findByIdIn_bulk() {
            // given
            Member m1 = memberRepository.save(newMember("u1", "N1"));
            Member m2 = memberRepository.save(newMember("u2", "N2"));

            // when
            List<Member> result = memberRepository.findByIdIn(List.of(m1.getId(), m2.getId()));

            // then (순서는 보장되지 않으므로 containsExactlyInAnyOrder)
            assertThat(result).extracting(Member::getId)
                .containsExactlyInAnyOrder(m1.getId(), m2.getId());
        }

    }

    @Nested
    @DisplayName("제약조건(Unique/NotNull) 검증")
    class Constraints {

        @Test
        @DisplayName("nick_name unique 제약 위반 시 예외가 발생한다.")
        void duplicate_nickName_violates_unique() {
            // given
            memberRepository.save(newMember("userA", "DUP"));

            // when & then (같은 닉네임인 경우 에러 발생)
            assertThatThrownBy(() -> {
                memberRepository.saveAndFlush(newMember("userB", "DUP"));

                // H2 + Hibernate에서 DataIntegrityViolationException 등 래핑
            }).isInstanceOfAny(RuntimeException.class);
        }

        @Test
        @DisplayName("password NOT NULL 제약 위반 시 예외가 발생한다.")
        void password_not_null_violation() {
            Member bad = Member.builder()
                .name("nonPwUser")
                .nickName("NP")     // password 누락
                .build();

            assertThatThrownBy(() -> {
                memberRepository.saveAndFlush(bad);
            }).isInstanceOfAny(RuntimeException.class);
        }

    }

}