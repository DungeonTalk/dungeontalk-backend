package org.com.dungeontalk.domain.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Optional;
import org.com.dungeontalk.domain.auth.entity.Auth;
import org.com.dungeontalk.domain.member.entity.Member;
import org.com.dungeontalk.global.support.TestJpaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestJpaConfig.class)   // 이것이 없으면 createdAt/updatedAt가 null!
class AuthRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private AuthRepository authRepository;

    @MockitoBean(name = "mongoMappingContext")
    MongoMappingContext mongoMappingContext;

    private Member persistMember(String name, String nick, String encPw) {
        Member member = Member.builder()
            .name(name)
            .nickName(nick)
            .password(encPw)
            .build();

        // ID는 @PrePersist로 UUIDv7 자동 생성됨
        return em.persistFlushFind(member);
    }

    private Auth persistAuth(Member member, String email, String access, String refresh) {
        Auth auth = Auth.builder()
            .member(member)
            .email(email)
            .tokenType("bearer")
            .accessToken(access)
            .refreshToken(refresh)
            .build();

        // 변경: 리포지토리로 저장 + 즉시 flush
        return authRepository.saveAndFlush(auth); // 또는 authRepository.findById(saved.getId()).orElseThrow()
    }

    @Test
    @DisplayName("PrePersist로 BaseEntity.id(UUIDv7)와 createdAt이 자동 생성된다.")
    void baseEntity_autoSave_onPersist() {
        Member alice = persistMember("alice", "Ali", "ENC");
        Auth auth = persistAuth(alice, "alice@example.com", "access.jwt", "refresh.jwt");

        assertThat(alice.getId()).isNotBlank();
        assertThat(auth.getId()).isNotBlank();

        assertThat(auth.getCreatedAt()).isNotNull();
        assertThat(auth.getUpdatedAt()).isNotNull(); // AuditingEntityListener 작동
        assertThat(auth.getCreatedAt()).isBeforeOrEqualTo(auth.getUpdatedAt());
    }

    @Test
    @DisplayName("멤버를 통해 auth 정보를 조회한다.")
    void findByMember() {
        Member member = persistMember("alice", "Ali", "enc");
        Auth auth = persistAuth(member, "alice@example.com", "access.jwt", "refresh.jwt");

        Optional<Auth> found = authRepository.findByMember(member);

        assertThat(found).isPresent();
        assertThat(found.get().getRefreshToken()).isEqualTo("refresh.jwt");
        assertThat(found.get().getMember().getId()).isEqualTo(member.getId());
    }

    @Test
    @DisplayName("멤버 ID로 auth 정보를 조회한다.")
    void findByMemberId() {
        Member bob = persistMember("bob", "Bobby", "enc2");
        persistAuth(bob, "bob@example.com", "access2.jwt", "refresh2.jwt");

        Optional<Auth> found = authRepository.findByMember_Id(bob.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("bob@example.com");
    }

    @Test
    @DisplayName("리프레시 토큰을 활용하여 auth 정보를 조회한다.")
    void findByRefreshToken() {
        Member member = persistMember("jonathan", "codesche", "enc3");
        persistAuth(member, "jonathan@example.com", "access3.jwt", "refresh3.jwt");

        assertThat(authRepository.findByRefreshToken("refresh3.jwt")).isPresent();
        assertThat(authRepository.findByRefreshToken("nope")).isNotPresent();
    }

    @Test
    @DisplayName("Auth 갱신: refreshToken 변경 후 영속화와 updatedAt 갱신 확인")
    void updateRefreshToken_updatesTimestamp() {
        Member d = persistMember("jonathan", "D", "ENC4");
        Auth auth = persistAuth(d, "jonathan@example.com", "access4.jwt", "refresh4.jwt");

        Instant beforeUpdate = auth.getUpdatedAt();

        auth.setAccessToken(null);
        auth.setRefreshToken("refresh4.new.jwt");
        Auth saved = em.persistFlushFind(auth);

        Optional<Auth> found = authRepository.findByMember_Id(d.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getAccessToken()).isNull();
        assertThat(found.get().getRefreshToken()).isEqualTo("refresh4.new.jwt");

        // updatedAt이 갱신되었는지(= 감사 필드 동작) 확인
        assertThat(found.get().getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
    }



}