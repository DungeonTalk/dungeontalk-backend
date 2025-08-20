package org.com.dungeontalk.domain.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import org.com.dungeontalk.domain.room.common.RoomType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class RoomServiceFactoryTest {

    @Mock
    RoomService aiRoomService;
    
    @Mock  
    RoomService chatRoomService;

    @Test
    @DisplayName("AI 룸 서비스 조회 성공 테스트")
    void getService_ai_success() {
        // given
        given(aiRoomService.getSupportedRoomType()).willReturn(RoomType.AI_GAME);
        given(chatRoomService.getSupportedRoomType()).willReturn(RoomType.PLAYER_CHAT);
        
        List<RoomService> roomServices = List.of(aiRoomService, chatRoomService);
        RoomServiceFactory factory = new RoomServiceFactory(roomServices);
        
        // when
        RoomService result = factory.getService(RoomType.AI_GAME);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(aiRoomService);
    }

    @Test
    @DisplayName("null 입력시 예외 발생 테스트")
    void getService_null_throwsException() {
        // given
        List<RoomService> roomServices = List.of();
        RoomServiceFactory factory = new RoomServiceFactory(roomServices);
        
        // when & then
        assertThatThrownBy(() -> factory.getService((RoomType) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("룸 타입이 null입니다");
    }

    @Test
    @DisplayName("지원되는 룸 타입 확인 테스트")
    void isSupported_true() {
        // given
        given(aiRoomService.getSupportedRoomType()).willReturn(RoomType.AI_GAME);
        
        List<RoomService> roomServices = List.of(aiRoomService);
        RoomServiceFactory factory = new RoomServiceFactory(roomServices);
        
        // when
        boolean result = factory.isSupported(RoomType.AI_GAME);
        
        // then
        assertThat(result).isTrue();
    }
}