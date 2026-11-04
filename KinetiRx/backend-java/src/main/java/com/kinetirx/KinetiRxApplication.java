package com.kinetirx;

import com.kinetirx.config.KinetiRxProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * KinetiRx backend entry point. Spring Boot port of the former Go service.
 * Same REST contract (see MIGRATION_PLAN.md), same environment variables.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(KinetiRxProperties.class)
public class KinetiRxApplication {

    public static void main(String[] args) {
        SpringApplication.run(KinetiRxApplication.class, args);
    }
}
