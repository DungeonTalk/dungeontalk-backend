package org.com.dungeontalk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@EnableJpaAuditing
@EnableMongoAuditing
@SpringBootApplication
public class DungeontalkApplication {

    public static void main(String[] args) {
        SpringApplication.run(DungeontalkApplication.class, args);
    }

}
