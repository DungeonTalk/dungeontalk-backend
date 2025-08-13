package org.com.dungeontalk.domain.stat.repository;

import org.com.dungeontalk.domain.stat.entity.RaceStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RaceStatsRepository extends JpaRepository<RaceStats, String> {

    @Query("SELECT r.race FROM RaceStats r")
    List<String> findAllRaceNames();

    Optional<RaceStats> findByRace(String race);
}
