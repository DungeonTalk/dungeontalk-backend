package org.com.dungeontalk.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.world.dto.response.WorldResponse;
import org.com.dungeontalk.domain.world.service.WorldService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.com.dungeontalk.global.security.CustomUserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Thymeleaf 뷰를 반환하는 컨트롤러
 * 페이지 라우팅 및 서버사이드 렌더링을 담당
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ViewController {
    
    private final WorldService worldService;

    /**
     * 메인 홈 페이지
     */
    @GetMapping("/")
    public String home(Model model) {
        // JWT 기반 인증이므로 클라이언트 사이드에서 토큰 체크
        return "index";
    }

    /**
     * 로그인 페이지
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * 게임 세계관 선택 페이지
     * Spring Security에서 인증 체크 (쿠키 기반)
     */
    @GetMapping("/game")
    @PreAuthorize("isAuthenticated()")
    public String game(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        // Spring Security가 쿠키 기반 인증을 처리
        if (userDetails != null) {
            log.info("게임 페이지 접근 - 사용자: {}", userDetails.getUsername());
            model.addAttribute("username", userDetails.getUsername());
        }
        
        // 세계관 목록을 서버에서 가져와서 Model에 추가
        try {
            List<WorldResponse> worlds = worldService.getAllWorlds();
            model.addAttribute("worlds", worlds);
            log.info("세계관 목록 로드 성공: {} 개", worlds.size());
        } catch (Exception e) {
            log.error("세계관 목록 로드 실패", e);
            model.addAttribute("worlds", List.of());
        }
        
        return "game-select";
    }
    
    /**
     * 게임 플레이 페이지
     * 실제 게임이 진행되는 페이지
     */
    @GetMapping("/game/play")
    @PreAuthorize("isAuthenticated()")
    public String gamePlay(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        // Spring Security가 쿠키 기반 인증을 처리
        if (userDetails != null) {
            log.info("게임 플레이 페이지 접근 - 사용자: {}", userDetails.getUsername());
            model.addAttribute("username", userDetails.getUsername());
        }
        
        return "game-play";
    }

    /**
     * 채팅 페이지
     */
    @GetMapping("/chat")
    public String chat(Model model) {
        // JWT 기반 인증이므로 클라이언트 사이드에서 토큰 체크
        return "chat";
    }

    /**
     * 프로필 페이지
     */
    @GetMapping("/profile")
    public String profile(Model model) {
        // JWT 기반 인증이므로 클라이언트 사이드에서 토큰 체크
        return "profile";
    }

    /**
     * 설정 페이지
     */
    @GetMapping("/settings")
    public String settings(Model model) {
        // JWT 기반 인증이므로 클라이언트 사이드에서 토큰 체크
        return "settings";
    }

    /**
     * 테스트 페이지 (개발용)
     */
    @GetMapping("/test")
    public String test() {
        return "test";
    }
}