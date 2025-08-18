package org.com.dungeontalk.domain.gamecharacter.repository;

import org.com.dungeontalk.domain.gamecharacter.entity.GameCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import io.lettuce.core.dynamic.annotation.Param;

import java.util.Optional;

public interface GameCharacterRepository extends JpaRepository<GameCharacter, String> {

    // 캐릭터 상세 조회, 스탯 계산 시 사용
    @Query("""
           select c
           from GameCharacter c
           left join fetch c.raceStats
           where c.id = :id
           """)
    Optional<GameCharacter> findWithRace(@Param("id") String id);

    // character 테이블에서 member_id로 캐릭터 조회 (MVP: 1개 멤버당 1개 캐릭터)
    Optional<GameCharacter> findByMemberId(String memberId);
    
    // member_id로 캐릭터 존재 여부 확인
    boolean existsByMemberId(String memberId);
}
