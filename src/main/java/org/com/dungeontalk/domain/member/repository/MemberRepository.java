package org.com.dungeontalk.domain.member.repository;

import org.com.dungeontalk.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, String> {

    // 중복 방지용 : 닉네임으로 유저 객체 찾기
    Optional<Member> findByNickName(String nickName);

    // 중복 방지용 : 아이디로 유저 객체 찾기
    Optional<Member> findByName(String name);


}
