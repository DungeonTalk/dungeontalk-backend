package org.com.dungeontalk.domain.aichat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AiServiceResponse {
    private String content;
    private Long responseTime;
    private List<String> sources;
}