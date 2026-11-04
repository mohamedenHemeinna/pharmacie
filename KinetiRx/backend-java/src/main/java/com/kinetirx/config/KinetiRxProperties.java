package com.kinetirx.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Runtime configuration. Every value comes from the environment (12-factor),
 * using the same variable names as the Go backend.
 */
@ConfigurationProperties(prefix = "kinetirx")
public record KinetiRxProperties(
        /** JWT_SECRET: HMAC key for HS256 tokens. Must be at least 16 characters. */
        String jwtSecret,
        /** Access-token lifetime (Go: 12h). */
        Duration jwtAccessTokenTtl,
        /** ALLOWED_ORIGINS: comma-separated CORS origins. */
        List<String> allowedOrigins,
        /** GEMINI_API_KEY: optional legacy AI fallback. */
        String geminiApiKey,
        /** KINETIRX_ADMIN_PASSWORD: optional first-boot admin password. */
        String adminBootstrapPassword,
        /** BACKUP_ENCRYPTION_KEY: hex-encoded 32-byte AES key. Optional. */
        String backupEncryptionKey
) {
    public KinetiRxProperties {
        // Same rule as the Go config loader: a secret is mandatory and at least 16 characters.
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new IllegalStateException("missing required environment variable: JWT_SECRET");
        }
        if (jwtSecret.length() < 16) {
            throw new IllegalStateException("JWT_SECRET must be at least 16 characters long (got " + jwtSecret.length() + ")");
        }
        if (jwtAccessTokenTtl == null) jwtAccessTokenTtl = Duration.ofHours(12);
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            allowedOrigins = List.of("http://localhost:5173", "http://localhost:3000");
        }
    }
}
