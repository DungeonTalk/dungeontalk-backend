package org.com.dungeontalk.domain.auth.repository;

import org.com.dungeontalk.domain.auth.entity.Auth;
import org.com.dungeontalk.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<Auth, String> {

    // 멤버 객체로 인증 객체 조회
    Optional<Auth> findByMember(Member member);

    // 멤버 고유번호로 인증 객체 조회
    Optional<Auth> findByMember_Id(String memberId);

    // 리프레시 토큰으로 인증 객체 조회
    Optional<Auth> findByRefreshToken(String refreshToken);

}
