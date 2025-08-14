package org.com.dungeontalk.domain.stat.controller;

import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.gamecharacter.repository.GameCharacterRepository;
import org.com.dungeontalk.domain.stat.entity.RaceStats;
import org.com.dungeontalk.domain.stat.repository.RaceStatsRepository;
import org.com.dungeontalk.global.rsData.RsData;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/init")
@RequiredArgsConstructor
@Profile({"dev", "local", "test"})
public class DataInitController {

    private final RaceStatsRepository raceStatsRepository;
    private final GameCharacterRepository gameCharacterRepository;

    // 기존 종족 스탯 데이터 모두 삭제
    @DeleteMapping("/race-stats")
    public RsData<String> clearRaceStats() {
        long count = raceStatsRepository.count();
        raceStatsRepository.deleteAll();
        String message = "삭제된 RaceStats 데이터: " + count + "개";
        return RsData.of("200", "RaceStats 삭제 완료", message);
    }

    // 테스트용 종족 스탯 데이터 생성 (엘프, 인간, 드워프)
    @PostMapping("/race-stats")
    public RsData<List<RaceStats>> createTestRaceStats() {
        // 엘프
        RaceStats elf = new RaceStats();
        elf.setRace("엘프");
        elf.setHp("100 + (wil * 10)");
        elf.setMp("150 + (wis * 12)");
        elf.setPhysicalAttack("str * 1.2");
        elf.setMagicAttack("int * 1.8");
        elf.setEvasionRate("dex * 1.5");
        elf.setAccuracy("50 + (dex * 0.8)");
        elf.setDiceOdds("lux * 0.15");

        // 인간
        RaceStats human = new RaceStats();
        human.setRace("인간");
        human.setHp("120 + (wil * 8)");
        human.setMp("100 + (wis * 10)");
        human.setPhysicalAttack("str * 1.5");
        human.setMagicAttack("int * 1.3");
        human.setEvasionRate("dex * 1.0");
        human.setAccuracy("60 + (dex * 1.0)");
        human.setDiceOdds("lux * 0.12");

        // 드워프
        RaceStats dwarf = new RaceStats();
        dwarf.setRace("드워프");
        dwarf.setHp("150 + (wil * 12)");
        dwarf.setMp("80 + (wis * 8)");
        dwarf.setPhysicalAttack("str * 1.8");
        dwarf.setMagicAttack("int * 1.0");
        dwarf.setEvasionRate("dex * 0.8");
        dwarf.setAccuracy("70 + (dex * 0.6)");
        dwarf.setDiceOdds("lux * 0.10");

        List<RaceStats> raceStatsList = List.of(elf, human, dwarf);
        var savedRaceStats = raceStatsRepository.saveAll(raceStatsList);
        return RsData.of("201", "RaceStats 생성 완료", savedRaceStats);
    }

    // 현재 등록된 모든 종족 스탯 데이터 조회
    @GetMapping("/race-stats")
    public RsData<List<RaceStats>> getAllRaceStats() {
        var raceStats = raceStatsRepository.findAll();
        return RsData.of("200", "RaceStats 조회 완료", raceStats);
    }

    // 기존 캐릭터 데이터 모두 삭제
    @DeleteMapping("/characters")
    public RsData<String> clearCharacters() {
        long count = gameCharacterRepository.count();
        gameCharacterRepository.deleteAll();
        String message = "삭제된 Character 데이터: " + count + "개";
        return RsData.of("200", "Character 삭제 완료", message);
    }

    // 모든 테스트 데이터 초기화 (캐릭터 삭제 → 종족 스탯 삭제 → 종족 스탯 생성)
    @PostMapping("/all")
    public RsData<String> initializeAllData() {
        // 1. 기존 캐릭터 삭제
        long characterCount = gameCharacterRepository.count();
        gameCharacterRepository.deleteAll();
        
        // 2. 기존 종족 스탯 삭제
        long raceStatsCount = raceStatsRepository.count();
        raceStatsRepository.deleteAll();
        
        // 3. 새로운 종족 스탯 생성
        createTestRaceStats();
        
        String message = String.format("데이터 초기화 완료 - 삭제된 캐릭터: %d개, 삭제된 종족 스탯: %d개, 새로 생성된 종족 스탯: 3개", 
                            characterCount, raceStatsCount);
        return RsData.of("200", "전체 데이터 초기화 완료", message);
    }
}