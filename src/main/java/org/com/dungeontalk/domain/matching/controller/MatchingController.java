package org.com.dungeontalk.domain.matching.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.matching.dto.request.MatchingCancelRequest;
import org.com.dungeontalk.domain.matching.dto.request.MatchingJoinRequest;
import org.com.dungeontalk.domain.matching.dto.response.MatchingCompleteResponse;
import org.com.dungeontalk.domain.matching.dto.response.MatchingStatusResponse;
import org.com.dungeontalk.domain.matching.dto.response.QueueStatsResponse;
import org.com.dungeontalk.domain.matching.service.MatchingService;
import org.com.dungeontalk.domain.matching.service.MatchingQueueManager;
import org.com.dungeontalk.domain.matching.common.WorldType;
import org.com.dungeontalk.global.rsData.RsData;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/matching")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;
    private final MatchingQueueManager queueManager;

    /**
     * 매칭 큐 참가
     */
    @PostMapping("/join")
    public RsData<MatchingStatusResponse> joinMatching(@Valid @RequestBody MatchingJoinRequest request) {
        log.info("매칭 참가 API 호출: userId={}, worldType={}", request.getUserId(), request.getWorldType());
        
        MatchingStatusResponse response = matchingService.joinMatching(request.getUserId(), request.getWorldType());
        return RsData.of("200", "매칭 큐 참가 완료", response);
    }

    /**
     * 매칭 취소
     */
    @DeleteMapping("/cancel")
    public RsData<String> cancelMatching(@Valid @RequestBody MatchingCancelRequest request) {
        log.info("매칭 취소 API 호출: userId={}", request.getUserId());
        
        boolean cancelled = matchingService.cancelMatching(request.getUserId());
        if (cancelled) {
            return RsData.of("200", "매칭 취소 완료", "SUCCESS");
        } else {
            return RsData.of("200", "매칭 대기 상태가 아니었습니다", "NOT_IN_QUEUE");
        }
    }

    /**
     * 사용자 매칭 상태 조회
     */
    @GetMapping("/status/{userId}")
    public RsData<MatchingStatusResponse> getMatchingStatus(@PathVariable String userId) {
        log.debug("매칭 상태 조회 API 호출: userId={}", userId);
        
        MatchingStatusResponse response = matchingService.getMatchingStatus(userId);
        return RsData.of("200", "매칭 상태 조회 성공", response);
    }

    /**
     * 전체 큐 현황 조회
     */
    @GetMapping("/queue/stats")
    public RsData<QueueStatsResponse> getQueueStats() {
        log.debug("큐 통계 조회 API 호출");
        
        QueueStatsResponse response = matchingService.getQueueStats();
        return RsData.of("200", "큐 통계 조회 성공", response);
    }

    /**
     * Redis 큐 초기화 (개발/테스트용)
     */
    @DeleteMapping("/queue/clear")
    public RsData<String> clearQueue() {
        log.info("Redis 큐 초기화 API 호출");
        
        try {
            for (WorldType worldType : WorldType.values()) {
                queueManager.clearQueue(worldType);
            }
            return RsData.of("200", "모든 큐가 초기화되었습니다", "SUCCESS");
        } catch (Exception e) {
            log.error("큐 초기화 중 오류 발생", e);
            return RsData.of("500", "큐 초기화 실패", "FAILED");
        }
    }

    /**
     * 수동 매칭 처리 (관리자용 - 테스트/디버깅 목적)
     */
    @PostMapping("/process/{worldType}")
    public RsData<?> processMatching(@PathVariable String worldType) {
        log.info("수동 매칭 처리 API 호출: worldType={}", worldType);
        
        try {
            org.com.dungeontalk.domain.matching.common.WorldType world = 
                    org.com.dungeontalk.domain.matching.common.WorldType.valueOf(worldType.toUpperCase());
            
            MatchingCompleteResponse response = matchingService.processMatching(world);
            
            if (response != null) {
                return RsData.of("200", "매칭 처리 완료", response);
            } else {
                return RsData.of("400", "매칭 대상이 부족합니다", "INSUFFICIENT_PARTICIPANTS");
            }
            
        } catch (IllegalArgumentException e) {
            return RsData.of("400", "잘못된 세계관 타입입니다", "INVALID_WORLD_TYPE");
        }
    }
}