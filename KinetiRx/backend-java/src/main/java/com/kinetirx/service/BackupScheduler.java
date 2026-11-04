package com.kinetirx.service;

import com.kinetirx.common.SecretCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Hourly check: runs a backup when the configured interval has passed since the last verified one.
 * "Due" is read from the database, so restarts never reset the schedule. Only active when the encryption key is set.
 */
@Component
public class BackupScheduler {

    private static final Logger log = LoggerFactory.getLogger(BackupScheduler.class);

    private final BackupService backups;
    private final SecretCipher cipher;

    public BackupScheduler(BackupService backups, SecretCipher cipher) {
        this.backups = backups;
        this.cipher = cipher;
    }

    @Scheduled(initialDelay = 15_000, fixedDelay = 3_600_000)
    public void checkAndRun() {
        if (!cipher.isConfigured()) return;
        BackupService.Destination dest;
        try {
            dest = backups.destination();
        } catch (BackupService.NotConfiguredException e) {
            return;
        } catch (RuntimeException e) {
            log.warn("backup scheduler: could not load config: {}", e.getMessage());
            return;
        }
        Instant last = backups.lastVerified();
        boolean due = last == null || Duration.between(last, Instant.now()).compareTo(Duration.ofDays(dest.intervalDays())) >= 0;
        if (!due) return;
        log.info("backup scheduler: running scheduled backup (interval={}d, last verified={})", dest.intervalDays(), last);
        try {
            backups.run(dest, "scheduled");
            log.info("backup scheduler: scheduled backup completed and verified");
        } catch (RuntimeException e) {
            log.error("backup scheduler: scheduled backup failed: {}", e.getMessage());
        }
    }
}
