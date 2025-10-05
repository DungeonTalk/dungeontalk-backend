package org.com.dungeontalk.domain.member.repository;

import java.util.List;
import org.com.dungeontalk.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, String> {

    // 중복 방지용 : 닉네임으로 유저 객체 찾기
    Optional<Member> findByNickName(String nickName);

    // 중복 방지용 : 아이디로 유저 객체 찾기
    Optional<Member> findByName(String name);

    // 회원 닉네임 일괄 조회 (채팅방 내 메시지 페이징 조회)
    List<Member> findByIdIn(List<String> ids);


}
