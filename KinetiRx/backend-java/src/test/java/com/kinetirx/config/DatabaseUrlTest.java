package com.kinetirx.config;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseUrlTest {

    @Test
    void convertsGoDatabaseUrlToJdbc() {
        Map<String, Object> props = new HashMap<>();
        String jdbc = DatabaseUrlEnvironmentPostProcessor.toJdbc(
                "postgres://kinetirx:s3cret@db.internal:5433/pharma?sslmode=disable", props);
        assertEquals("jdbc:postgresql://db.internal:5433/pharma?sslmode=disable", jdbc);
        assertEquals("kinetirx", props.get("spring.datasource.username"));
        assertEquals("s3cret", props.get("spring.datasource.password"));
    }

    @Test
    void defaultsPortAndDecodesEscapedPassword() {
        Map<String, Object> props = new HashMap<>();
        String jdbc = DatabaseUrlEnvironmentPostProcessor.toJdbc(
                "postgresql://app:p%40ss%23word@localhost/kinetirx", props);
        assertEquals("jdbc:postgresql://localhost:5432/kinetirx", jdbc);
        assertEquals("p@ss#word", props.get("spring.datasource.password"));
    }
}
