package com.kinetirx.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/** GET /api/health — the only route reachable without a token besides login and setup. */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "ok",
                "timestamp", Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
    }
}
