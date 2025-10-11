package org.com.dungeontalk.domain.aichat.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

class AiServiceRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Builder로 객체 생성 테스트")
    void builderCreatesObject() {
        // given
        String gameId = "test-game-123";
        String aiGameRoomId = "test-room-456";
        String currentUser = "user-789";
        String currentMessage = "안녕하세요!";
        int turnNumber = 5;

        // when
        AiServiceRequest result = AiServiceRequest.builder()
                .gameId(gameId)
                .aiGameRoomId(aiGameRoomId)
                .currentUser(currentUser)
                .currentMessage(currentMessage)
                .turnNumber(turnNumber)
                .build();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getGameId()).isEqualTo(gameId);
        assertThat(result.getAiGameRoomId()).isEqualTo(aiGameRoomId);
        assertThat(result.getCurrentUser()).isEqualTo(currentUser);
        assertThat(result.getCurrentMessage()).isEqualTo(currentMessage);
        assertThat(result.getTurnNumber()).isEqualTo(turnNumber);
    }

    //실행되면 파이썬에서도 실행 가능함 왜 이 dto를 실행하느냐 하면  파이썬에서는 카멜 케이스가 아니라 스네이크로 받는데 dto 단에서 오류가
    // 발생할수도 있어서 현재 이렇게 했습니다.
    @Test
    @DisplayName("Java 객체를 JSON으로 변환 (@JsonProperty 매핑 확인)")
    void serializeToJson() throws Exception {
        // given
        AiServiceRequest request = AiServiceRequest.builder()
                .gameId("game-123")
                .aiGameRoomId("room-456")
                .currentUser("user-789")
                .currentMessage("테스트 메시지")
                .turnNumber(3)
                .contextMessages(List.of())
                .build();

        // when
        String json = objectMapper.writeValueAsString(request);

        // then - @JsonProperty로 설정한 snake_case 키들이 있는지 확인
        assertThat(json).contains("\"game_id\":\"game-123\"");
        assertThat(json).contains("\"ai_game_room_id\":\"room-456\"");
        assertThat(json).contains("\"current_user\":\"user-789\"");
        assertThat(json).contains("\"current_message\":\"테스트 메시지\"");
        assertThat(json).contains("\"turn_number\":3");
        assertThat(json).contains("\"context_messages\":[]");

        // camelCase가 아닌지도 확인 (혹시 @JsonProperty가 안 되었을 경우)
        assertThat(json).doesNotContain("gameId");
        assertThat(json).doesNotContain("aiGameRoomId");
    }

    @Test
    @DisplayName("JSON을 Java 객체로 변환 (snake_case → camelCase)")
    void deserializeFromJson() throws Exception {
        // given - Python AI 서비스에서 보낼 수 있는 JSON 형태
        String json = """
            {
                "game_id": "test-game",
                "ai_game_room_id": "test-room",
                "current_user": "test-user",
                "current_message": "안녕하세요",
                "turn_number": 7,
                "context_messages": []
            }
            """;

        // when
        AiServiceRequest result = objectMapper.readValue(json, AiServiceRequest.class);

        // then - @JsonProperty 매핑으로 올바르게 변환되었는지 확인
        assertThat(result).isNotNull();
        assertThat(result.getGameId()).isEqualTo("test-game");
        assertThat(result.getAiGameRoomId()).isEqualTo("test-room");
        assertThat(result.getCurrentUser()).isEqualTo("test-user");
        assertThat(result.getCurrentMessage()).isEqualTo("안녕하세요");
        assertThat(result.getTurnNumber()).isEqualTo(7);
        assertThat(result.getContextMessages()).isNotNull().isEmpty();
    }
}