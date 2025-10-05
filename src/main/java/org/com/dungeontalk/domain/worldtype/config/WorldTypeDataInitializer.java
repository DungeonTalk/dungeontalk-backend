package org.com.dungeontalk.domain.worldtype.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.worldtype.entity.WorldType;
import org.com.dungeontalk.domain.worldtype.repository.WorldTypeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 기존 하드코딩된 세계관 데이터를 DB로 초기화
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorldTypeDataInitializer implements CommandLineRunner {

    private final WorldTypeRepository worldTypeRepository;

    @Override
    public void run(String... args) {
        if (worldTypeRepository.count() == 0) {
            initializeWorldTypes();
            log.info("세계관 기본 데이터가 초기화되었습니다.");
        }
    }

    private void initializeWorldTypes() {
        // 기존 WorldType enum의 데이터를 DB에 초기화
        WorldType fantasy = WorldType.builder()
                .code("FANTASY")
                .displayName("판타지")
                .description("중세 판타지 - 마법과 모험의 세계")
                .gameSettings("중세 판타지 세계관에서 펼쳐지는 마법과 모험의 이야기")
                .isActive(true)
                .sortOrder(1)
                .build();

        WorldType sf = WorldType.builder()
                .code("SF")
                .displayName("SF")
                .description("미래 SF - 과학기술과 우주탐험")
                .gameSettings("미래 우주 세계관에서 펼쳐지는 과학기술과 탐험의 이야기")
                .isActive(true)
                .sortOrder(2)
                .build();

        WorldType modern = WorldType.builder()
                .code("MODERN")
                .displayName("현대")
                .description("현대 도시 - 일상과 미스터리")
                .gameSettings("현대 도시 세계관에서 펼쳐지는 일상과 미스터리의 이야기")
                .isActive(true)
                .sortOrder(3)
                .build();

        worldTypeRepository.save(fantasy);
        worldTypeRepository.save(sf);
        worldTypeRepository.save(modern);
    }
}