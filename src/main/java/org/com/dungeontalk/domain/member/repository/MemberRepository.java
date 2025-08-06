package org.com.dungeontalk.domain.member.repository;

import org.com.dungeontalk.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, String> {

}
