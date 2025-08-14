package org.com.dungeontalk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableMongoAuditing
@EnableScheduling
@SpringBootApplication
public class DungeontalkApplication {

    public static void main(String[] args) {
        SpringApplication.run(DungeontalkApplication.class, args);
    }

}
