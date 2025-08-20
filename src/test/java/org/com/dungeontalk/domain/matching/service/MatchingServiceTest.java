package org.com.dungeontalk.domain.matching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import org.com.dungeontalk.domain.aichat.service.AiGameRoomService;
import org.com.dungeontalk.domain.chat.service.ChatRoomService;
import org.com.dungeontalk.domain.room.service.RoomServiceFactory;
import org.com.dungeontalk.domain.matching.common.MatchingStatus;
import org.com.dungeontalk.domain.matching.common.WorldType;
import org.com.dungeontalk.domain.matching.dto.response.MatchingStatusResponse;
import org.com.dungeontalk.global.exception.customException.AiChatException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    MatchingQueueManager queueManager;
    
    @Mock
    AiGameRoomService aiGameRoomService;
    
    @Mock
    ChatRoomService chatRoomService;
    
    @Mock
    StringRedisTemplate redisTemplate;
    
    @Mock
    MatchingWebSocketService webSocketService;
    
    @Mock
    RoomServiceFactory roomServiceFactory;

    @InjectMocks
    MatchingService matchingService;

    @Test
    @DisplayName("매칭 참가 성공 테스트")
    void joinMatching_success() {
        // given
        String memberId = "user-123";
        WorldType worldType = WorldType.FANTASY;
        
        given(queueManager.isUserInQueue(memberId)).willReturn(false);
        given(queueManager.getQueueSize(worldType)).willReturn(5);
        given(queueManager.getUserQueuePosition(memberId, worldType)).willReturn(6);
        given(queueManager.canProcessMatching(worldType)).willReturn(false);
        
        // when
        MatchingStatusResponse result = matchingService.joinMatching(memberId, worldType);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.getMemberId()).isEqualTo(memberId);
        assertThat(result.getWorldType()).isEqualTo(worldType);
        assertThat(result.getStatus()).isEqualTo(MatchingStatus.WAITING);
        assertThat(result.getCurrentPosition()).isEqualTo(6);
        assertThat(result.getTotalInQueue()).isEqualTo(5);
        
        then(queueManager).should().addToQueue(memberId, worldType);
        then(queueManager).should().getQueueSize(worldType);
        then(queueManager).should().getUserQueuePosition(memberId, worldType);
    }

    @Test
    @DisplayName("매칭 참가 실패 - 이미 큐에 있는 사용자")
    void joinMatching_failure_alreadyInQueue() {
        // given
        String memberId = "user-123";
        WorldType worldType = WorldType.FANTASY;
        
        given(queueManager.isUserInQueue(memberId)).willReturn(true);
        
        // when & then
        assertThatThrownBy(() -> matchingService.joinMatching(memberId, worldType))
                .isInstanceOf(AiChatException.class);
        
        then(queueManager).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("매칭 취소 성공 테스트")
    void cancelMatching_success() {
        // given
        String memberId = "user-123";
        WorldType worldType = WorldType.FANTASY;
        
        Map<Object, Object> userInfo = new HashMap<>();
        userInfo.put("worldType", worldType.name());
        userInfo.put("status", "WAITING");
        
        given(queueManager.getUserMatchingInfo(memberId)).willReturn(userInfo);
        given(queueManager.removeFromQueue(memberId)).willReturn(true);
        given(queueManager.getQueueSize(worldType)).willReturn(4);
        
        // when
        boolean result = matchingService.cancelMatching(memberId);
        
        // then
        assertThat(result).isTrue();
        then(queueManager).should().removeFromQueue(memberId);
        then(webSocketService).should().sendMatchingCancelled(memberId, worldType);
    }

    @Test
    @DisplayName("매칭 취소 실패 - 사용자가 대기 중이 아님")
    void cancelMatching_failure_notInQueue() {
        // given
        String memberId = "user-123";
        
        given(queueManager.getUserMatchingInfo(memberId)).willReturn(new HashMap<>());
        given(queueManager.removeFromQueue(memberId)).willReturn(false);
        
        // when
        boolean result = matchingService.cancelMatching(memberId);
        
        // then
        assertThat(result).isFalse();
        then(queueManager).should().removeFromQueue(memberId);
    }

    @Test
    @DisplayName("매칭 상태 조회 성공 테스트")
    void getMatchingStatus_success() {
        // given
        String memberId = "user-123";
        WorldType worldType = WorldType.FANTASY;
        MatchingStatus status = MatchingStatus.WAITING;
        Instant joinedAt = Instant.now();
        
        Map<Object, Object> userInfo = new HashMap<>();
        userInfo.put("worldType", worldType.name());
        userInfo.put("status", status.name());
        userInfo.put("joinedAt", joinedAt.toString());
        
        given(queueManager.getUserMatchingInfo(memberId)).willReturn(userInfo);
        given(queueManager.getQueueSize(worldType)).willReturn(5);
        given(queueManager.getUserQueuePosition(memberId, worldType)).willReturn(3);
        
        // when
        MatchingStatusResponse result = matchingService.getMatchingStatus(memberId);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.getMemberId()).isEqualTo(memberId);
        assertThat(result.getWorldType()).isEqualTo(worldType);
        assertThat(result.getStatus()).isEqualTo(status);
        assertThat(result.getCurrentPosition()).isEqualTo(3);
        assertThat(result.getTotalInQueue()).isEqualTo(5);
    }

    @Test
    @DisplayName("매칭 상태 조회 실패 - 사용자 정보 없음")
    void getMatchingStatus_failure_noUserInfo() {
        // given
        String memberId = "user-123";
        
        given(queueManager.getUserMatchingInfo(memberId)).willReturn(new HashMap<>());
        
        // when & then
        assertThatThrownBy(() -> matchingService.getMatchingStatus(memberId))
                .isInstanceOf(AiChatException.class);
    }
}