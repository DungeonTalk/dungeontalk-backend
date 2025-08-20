package org.com.dungeontalk.domain.stat.repository;

import org.com.dungeontalk.domain.stat.entity.RaceStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RaceStatsRepository extends JpaRepository<RaceStats, String> {

    // 모든 종족명만 조회 (캐릭터 생성 시 종족 선택용)
    @Query("SELECT r.race FROM RaceStats r")
    List<String> findAllRaceNames();

    // 종족명으로 종족 스탯 정보 조회 (스탯 공식 포함)
    Optional<RaceStats> findByRace(String race);
}
