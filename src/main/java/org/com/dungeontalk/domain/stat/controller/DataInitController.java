package org.com.dungeontalk.domain.stat.controller;

import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.gamecharacter.repository.GameCharacterRepository;
import org.com.dungeontalk.domain.stat.entity.RaceStats;
import org.com.dungeontalk.domain.stat.repository.RaceStatsRepository;
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

    // 기존 데이터 모두 삭제
    @DeleteMapping("/race-stats")
    public String clearRaceStats() {
        long count = raceStatsRepository.count();
        raceStatsRepository.deleteAll();
        return "삭제된 RaceStats 데이터: " + count + "개";
    }

    // 테스트용 RaceStats 데이터 생성
    @PostMapping("/race-stats")
    public List<RaceStats> createTestRaceStats() {
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
        return raceStatsRepository.saveAll(raceStatsList);
    }

    // 현재 RaceStats 데이터 확인
    @GetMapping("/race-stats")
    public List<RaceStats> getAllRaceStats() {
        return raceStatsRepository.findAll();
    }

    // 캐릭터 데이터 모두 삭제
    @DeleteMapping("/characters")
    public String clearCharacters() {
        long count = gameCharacterRepository.count();
        gameCharacterRepository.deleteAll();
        return "삭제된 Character 데이터: " + count + "개";
    }

    // 모든 테스트 데이터 초기화 (권장 순서)
    @PostMapping("/all")
    public String initializeAllData() {
        // 1. 기존 캐릭터 삭제
        long characterCount = gameCharacterRepository.count();
        gameCharacterRepository.deleteAll();
        
        // 2. 기존 종족 스탯 삭제
        long raceStatsCount = raceStatsRepository.count();
        raceStatsRepository.deleteAll();
        
        // 3. 새로운 종족 스탯 생성
        createTestRaceStats();
        
        return String.format("데이터 초기화 완료 - 삭제된 캐릭터: %d개, 삭제된 종족 스탯: %d개, 새로 생성된 종족 스탯: 3개", 
                            characterCount, raceStatsCount);
    }
}