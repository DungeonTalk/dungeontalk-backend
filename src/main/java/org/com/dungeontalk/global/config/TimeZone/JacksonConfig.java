package org.com.dungeontalk.global.config.TimeZone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
public class JacksonConfig {

    private final ObjectMapper objectMapper;

    public JacksonConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void setup() {
        objectMapper.registerModule(new JavaTimeModule());

        SimpleModule module = new SimpleModule();
        module.addSerializer(Instant.class, new InstantToKstSerializer());
        module.addDeserializer(Instant.class, new InstantToKstDeserializer());
        objectMapper.registerModule(module);

        objectMapper.setTimeZone(TimeZone.getTimeZone(ZoneId.of("Asia/Seoul")));
    }
}
