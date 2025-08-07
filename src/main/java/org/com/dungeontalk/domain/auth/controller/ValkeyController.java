package org.com.dungeontalk.domain.auth.controller;

import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.auth.service.ValkeyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1/valkey")
@RequiredArgsConstructor
public class ValkeyController {

    private final ValkeyService valkeyService;

    // 현재 우리 세션 valkey에 등록된 유저들의 토큰을 모두 조회하는 컨트롤러
    @GetMapping("/session/all")
    public Map<String, String> getAllSessionData() {
        return valkeyService.getAllSessionRedisData();
    }
}
