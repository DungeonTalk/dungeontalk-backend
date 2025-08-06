package org.com.dungeontalk.domain.auth.repository;

import org.com.dungeontalk.domain.auth.entity.Auth;
import org.com.dungeontalk.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<Auth, String> {

    Optional<Auth> findByMember(Member member);

    Optional<Auth> findByMember_Id(String memberId);
}
