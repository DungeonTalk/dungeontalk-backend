package org.com.dungeontalk.domain.gamecharacter.repository;

import org.com.dungeontalk.domain.gamecharacter.entity.GameCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // io.lettuce.core.dynamic.annotation.Param -> 스프링 파라미터로 변경

import java.util.Optional;

public interface GameCharacterRepository extends JpaRepository<GameCharacter, String> {

    // Member 정보도 함께 가져오도록 fetch join 추가 (N+1 방지)
    @Query("""
           select c
           from GameCharacter c
           left join fetch c.raceStats
           left join fetch c.member
           where c.id = :id
           """)
    Optional<GameCharacter> findWithRace(@Param("id") String id);

    // character 테이블에서 member_id로 캐릭터 조회
    Optional<GameCharacter> findByMemberId(String memberId);

    // member_id로 캐릭터 존재 여부 확인
    boolean existsByMemberId(String memberId);
}
