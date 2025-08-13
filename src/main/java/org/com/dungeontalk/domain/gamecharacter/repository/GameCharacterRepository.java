package org.com.dungeontalk.domain.gamecharacter.repository;

import org.com.dungeontalk.domain.gamecharacter.entity.GameCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import io.lettuce.core.dynamic.annotation.Param;

import java.util.List;
import java.util.Optional;

public interface GameCharacterRepository extends JpaRepository<GameCharacter, String> {
    @Query("""
           select c
           from GameCharacter c
           left join fetch c.raceStats
           where c.id = :id
           """)
    Optional<GameCharacter> findWithRace(@Param("id") String id);

    List<GameCharacter> findByMemberId(String memberId);
}
