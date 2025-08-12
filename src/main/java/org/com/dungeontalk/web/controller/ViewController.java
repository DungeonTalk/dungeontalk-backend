package org.com.dungeontalk.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Thymeleaf 뷰를 반환하는 컨트롤러
 * 페이지 라우팅 및 서버사이드 렌더링을 담당
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ViewController {

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
     * 게임 페이지
     */
    @GetMapping("/game")
    public String game(Model model) {
        // JWT 기반 인증이므로 클라이언트 사이드에서 토큰 체크
        return "game";
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