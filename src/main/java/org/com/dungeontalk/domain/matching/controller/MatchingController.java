package org.com.dungeontalk.domain.matching.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "게임 매칭", description = "게임 매칭 관련 API")
@Slf4j
@RestController
@RequestMapping("/v1/match")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;
    private final MatchingQueueManager queueManager;

    @Operation(summary = "매칭 큐 참가", description = "게임 매칭 큐에 참가합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "매칭 큐 참가 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "409", description = "이미 매칭 대기 중")
    })
    @PostMapping("/join")
    public RsData<MatchingStatusResponse> joinMatching(@Valid @RequestBody MatchingJoinRequest request) {
        log.info("매칭 참가 API 호출: memberId={}, worldType={}", request.getMemberId(), request.getWorldType());
        
        MatchingStatusResponse response = matchingService.joinMatching(request.getMemberId(), request.getWorldType());
        return RsData.of("200", "매칭 큐 참가 완료", response);
    }

    @Operation(summary = "매칭 취소", description = "매칭 큐에서 탈퇴합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "매칭 취소 성공 또는 대기 상태가 아님")
    })
    @DeleteMapping("/cancel")
    public RsData<String> cancelMatching(@Valid @RequestBody MatchingCancelRequest request) {
        log.info("매칭 취소 API 호출: memberId={}", request.getMemberId());
        
        boolean cancelled = matchingService.cancelMatching(request.getMemberId());
        if (cancelled) {
            return RsData.of("200", "매칭 취소 완료", "SUCCESS");
        } else {
            return RsData.of("200", "매칭 대기 상태가 아니었습니다", "NOT_IN_QUEUE");
        }
    }

    @Operation(summary = "매칭 상태 조회", description = "사용자의 현재 매칭 상태를 조회합니다")
    @GetMapping("/status/{memberId}")
    public RsData<MatchingStatusResponse> getMatchingStatus(
            @Parameter(description = "회원 ID", required = true) @PathVariable String memberId) {
        log.debug("매칭 상태 조회 API 호출: memberId={}", memberId);
        
        MatchingStatusResponse response = matchingService.getMatchingStatus(memberId);
        return RsData.of("200", "매칭 상태 조회 성공", response);
    }

    @Operation(summary = "큐 현황 조회", description = "전체 매칭 큐의 현황을 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "큐 현황 조회 성공")
    })
    @GetMapping("/queue/stats")
    public RsData<QueueStatsResponse> getQueueStats() {
        log.debug("큐 통계 조회 API 호출");
        
        QueueStatsResponse response = matchingService.getQueueStats();
        return RsData.of("200", "큐 통계 조회 성공", response);
    }

    @Operation(summary = "큐 초기화", description = "Redis 매칭 큐를 초기화합니다 (개발/테스트용)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "큐 초기화 성공"),
        @ApiResponse(responseCode = "500", description = "큐 초기화 실패")
    })
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

    @Operation(summary = "수동 매칭 처리", description = "수동으로 매칭을 처리합니다 (관리자/테스트용)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "매칭 처리 성공"),
        @ApiResponse(responseCode = "400", description = "매칭 대상 부족 또는 잘못된 요청")
    })
    @PostMapping("/process/{worldType}")
    public RsData<?> processMatching(
            @Parameter(description = "세계관 타입", required = true, example = "FANTASY") @PathVariable String worldType) {
        log.info("수동 매칭 처리 API 호출: worldType={}", worldType);
        
        try {
            WorldType world = WorldType.valueOf(worldType.toUpperCase());
            
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