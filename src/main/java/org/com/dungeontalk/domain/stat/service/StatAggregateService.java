package org.com.dungeontalk.domain.stat.service;

import jakarta.persistence.Column;
import org.com.dungeontalk.domain.gamecharacter.entity.GameCharacter;
import org.com.dungeontalk.domain.gamecharacter.repository.GameCharacterRepository;
import org.com.dungeontalk.domain.stat.entity.RaceStats;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class StatAggregateService {

    private final GameCharacterRepository gameCharacterRepository;
    private final StatCalculatorService calculator;

    public StatAggregateService(GameCharacterRepository gameCharacterRepository,
                                StatCalculatorService calculator) {
        this.gameCharacterRepository = gameCharacterRepository;
        this.calculator = calculator;
    }

    // DB 1쿼리(fetch join) + 메모리에서 전 스탯 계산
    @Transactional(readOnly = true)
    public Map<String, Double> calculateAllStats(String characterId) {
        GameCharacter gameCharacter = gameCharacterRepository.findWithRace(characterId)
                .orElseThrow(() -> new IllegalArgumentException("캐릭터 없음: " + characterId));

        RaceStats raceStats = gameCharacter.getRaceStats();
        if (raceStats == null) {
            throw new IllegalArgumentException("종족 스탯 공식 없음: " + gameCharacter.getRaceTypeId());
        }

        Map<String, Object> variables = gameCharacter.toVariableMap();   // 필요 변수 준비(검증 추가는 이후 단계에서)
        Map<String, String> formulas  = extractFormulas(raceStats);  // 동적 추출(키=@Column name)

        Map<String, Double> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : formulas.entrySet()) {
            double value = calculator.calculate(e.getValue(), variables);

            // 스네이크케이스를 카멜케이스로 변환
            String camelCaseKey = toCamelCase(e.getKey());
            result.put(camelCaseKey, value);
        }
        return result;
    }

    // RaceStats의 String 필드 중 수식으로 쓰는 것만 수집 (키는 @Column name, 없으면 필드명)
    private Map<String, String> extractFormulas(RaceStats rs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (Field f : rs.getClass().getDeclaredFields()) {
            if (f.getType() != String.class) continue;     // 숫자/날짜 등 제외
            if ("race".equals(f.getName())) continue;      // 수식이 아닌 필드 제외(필요시 추가)

            f.setAccessible(true);
            try {
                String formula = (String) f.get(rs);
                if (formula == null || formula.isBlank()) continue;

                Column col = f.getAnnotation(Column.class);
                String key = (col != null && !col.name().isBlank()) ? col.name() : f.getName();
                map.put(key, formula); // ex) "physical_attack" -> "str*1.2 + dex*0.5"
            } catch (IllegalAccessException ignore) {}
        }
        return map;
    }

    // 스네이크케이스를 카멜케이스로 변환
    private String toCamelCase(String snakeCase) {
        if (snakeCase == null || snakeCase.isEmpty()) {
            return snakeCase;
        }

        StringBuilder result = new StringBuilder();
        String[] words = snakeCase.split("_");

        // 첫 번째 단어는 그대로, 나머지는 첫 글자만 대문자
        result.append(words[0]);
        for (int i = 1; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                result.append(Character.toUpperCase(words[i].charAt(0)));
                if (words[i].length() > 1) {
                    result.append(words[i].substring(1));
                }
            }
        }

        return result.toString();
    }
}