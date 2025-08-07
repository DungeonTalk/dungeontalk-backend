package org.com.dungeontalk.domain.auth.controller;

import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.auth.service.ValkeyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/v1/valkey")
@RequiredArgsConstructor
public class ValkeyController {

    private final ValkeyService valkeyService;

    // 테스트용 세션 데이터 저장 (Key, Value 입력)
    @GetMapping("/session/test/save")
    public String saveTestSessionData() {
        String key = "test-key" + LocalDateTime.now();
        String value = LocalDateTime.now().toString();
        valkeyService.saveSessionData(key, value);
        return "Saved test session data: " + key + " = " + value;
    }

    // 현재 우리 세션 valkey에 등록된 유저들의 토큰을 모두 조회하는 컨트롤러
    @GetMapping("/session/all")
    public Map<String, String> getAllSessionData() {

        return valkeyService.getAllTestKeySessionData();
    }

    // 현재 모든 세션의 데이터를 키값만 조회
    @GetMapping("/session/keys")
    public Set<String> getAllSessionKeys() {
        return valkeyService.getAllSessionKeys();
    }

}
