package org.com.dungeontalk.global.config.TimeZone;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class InstantToKstDeserializer extends StdDeserializer<Instant> {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.of("Asia/Seoul"));

    public InstantToKstDeserializer() {
        super(Instant.class);
    }

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();

        // KST 포맷 파싱
        LocalDateTime localDateTime = LocalDateTime.parse(text, FORMATTER);
        return localDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant();
    }
}
