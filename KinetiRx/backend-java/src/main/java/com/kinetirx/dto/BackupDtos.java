package com.kinetirx.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/** Backup wire shapes. Omitted fields match the Go omitempty rules. */
public final class BackupDtos {

    private BackupDtos() {}

    public record ConfigInput(String endpoint, String bucket, String region, String accessKeyId,
                              String secretAccessKey, Integer intervalDays, Integer retainCount) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ConfigStatus(boolean configured, String endpoint, String bucket, String region,
                               String accessKeyId, Integer intervalDays, Integer retainCount, Instant updatedAt) {

        public static ConfigStatus notConfigured() {
            return new ConfigStatus(false, null, null, null, null, null, null, null);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Record(String id, String source, String status, String s3Key, Long sizeBytes, String sha256,
                         String error, Instant startedAt, Instant completedAt) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Status(ConfigStatus config, Instant lastVerifiedAt, Instant nextDueAt) {}

    public record RestoreRequest(String confirm) {}
}
