package org.com.dungeontalk.domain.aichat.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Jacksonized
public class AiServiceRequest {
    @JsonProperty("game_id")
    private String gameId;
    
    @JsonProperty("ai_game_room_id")
    private String aiGameRoomId;
    
    @JsonProperty("current_user")
    private String currentUser;
    
    @JsonProperty("current_message")
    private String currentMessage;
    
    @JsonProperty("context_messages")
    private List<ContextMessage> contextMessages;
    
    @JsonProperty("turn_number")
    private int turnNumber;
    
    @JsonProperty("game_settings")
    private String gameSettings;
}