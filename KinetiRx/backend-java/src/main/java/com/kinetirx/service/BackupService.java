package com.kinetirx.service;

import com.kinetirx.common.SecretCipher;
import com.kinetirx.dto.BackupDtos;
import com.kinetirx.entity.BackupConfig;
import com.kinetirx.entity.BackupHistory;
import com.kinetirx.exception.ApiException;
import com.kinetirx.repository.BackupConfigRepository;
import com.kinetirx.repository.BackupHistoryRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Offsite backup: pg_dump (custom format) -> S3 upload -> download back and compare SHA-256 ->
 * record the result -> keep only the newest N verified backups. Restores are verified before they touch the database.
 */
@Service
public class BackupService {

    /** Thrown when no backup destination has been saved yet. */
    public static class NotConfiguredException extends RuntimeException {}

    private final BackupConfigRepository configs;
    private final BackupHistoryRepository history;
    private final SecretCipher cipher;
    private final DbTools db;

    public BackupService(BackupConfigRepository configs, BackupHistoryRepository history,
                         SecretCipher cipher, DbTools db) {
        this.configs = configs;
        this.history = history;
        this.cipher = cipher;
        this.db = db;
    }

    /** Decrypted destination, for internal use only. Never returned from an endpoint. */
    public record Destination(String endpoint, String bucket, String region, String accessKeyId,
                              String secretAccessKey, int intervalDays, int retainCount) {}

    // ---- configuration ---------------------------------------------------

    @Transactional(readOnly = true)
    public BackupDtos.ConfigStatus status() {
        return configs.findById(Boolean.TRUE)
                .map(c -> new BackupDtos.ConfigStatus(true, c.getEndpoint(), c.getBucket(), c.getRegion(),
                        c.getAccessKeyId(), c.getIntervalDays(), c.getRetainCount(), c.getUpdatedAt()))
                .orElseGet(BackupDtos.ConfigStatus::notConfigured);
    }

    @Transactional
    public BackupDtos.ConfigStatus save(BackupDtos.ConfigInput in) {
        cipher.requireConfigured();
        if (blank(in.endpoint()) || blank(in.bucket()) || blank(in.accessKeyId()) || blank(in.secretAccessKey())) {
            throw ApiException.badRequest("Invalid backup config payload: endpoint, bucket, accessKeyId and secretAccessKey are required");
        }
        String region = blank(in.region()) ? "auto" : in.region();
        int interval = in.intervalDays() == null || in.intervalDays() <= 0 ? 3 : Math.min(in.intervalDays(), 30);
        int retain = in.retainCount() == null || in.retainCount() <= 0 ? 5 : Math.min(in.retainCount(), 30);
        Destination dest = new Destination(in.endpoint(), in.bucket(), region, in.accessKeyId(),
                in.secretAccessKey(), interval, retain);
        try {
            try (S3Client s3 = s3(dest)) {
                s3.headBucket(HeadBucketRequest.builder().bucket(dest.bucket()).build());
            }
        } catch (RuntimeException e) {
            throw ApiException.badRequest("Could not connect with these details: could not reach bucket \""
                    + dest.bucket() + "\" at \"" + dest.endpoint() + "\"");
        }
        BackupConfig c = configs.findById(Boolean.TRUE).orElseGet(BackupConfig::new);
        c.setEndpoint(dest.endpoint());
        c.setBucket(dest.bucket());
        c.setRegion(region);
        c.setAccessKeyId(dest.accessKeyId());
        c.setSecretAccessKeyCipher(cipher.encrypt(dest.secretAccessKey()));
        c.setIntervalDays(interval);
        c.setRetainCount(retain);
        configs.saveAndFlush(c);
        return status();
    }

    @Transactional
    public void deleteConfig() {
        configs.deleteById(Boolean.TRUE);
    }

    @Transactional(readOnly = true)
    public Destination destination() {
        BackupConfig c = configs.findById(Boolean.TRUE).orElseThrow(NotConfiguredException::new);
        return new Destination(c.getEndpoint(), c.getBucket(), c.getRegion(), c.getAccessKeyId(),
                cipher.decrypt(c.getSecretAccessKeyCipher()), c.getIntervalDays(), c.getRetainCount());
    }

    @Transactional(readOnly = true)
    public List<BackupDtos.Record> list() {
        List<BackupHistory> rows = history.findAllByOrderByStartedAtDesc(Limit.of(50));
        return rows.isEmpty() ? null : rows.stream().map(BackupService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public BackupDtos.Status overallStatus() {
        BackupDtos.ConfigStatus cs = status();
        Instant lastVerified = lastVerified();
        return new BackupDtos.Status(cs, lastVerified,
                lastVerified != null && cs.configured() ? lastVerified.plus(Duration.ofDays(cs.intervalDays())) : null);
    }

    public Instant lastVerified() {
        return history.findFirstByStatusOrderByCompletedAtDesc("verified")
                .map(BackupHistory::getCompletedAt).orElse(null);
    }

    public BackupDtos.Record get(String id) {
        return history.findById(id).map(BackupService::toDto).orElse(null);
    }

    // ---- running and restoring ------------------------------------------

    /** Runs one full backup and records every step, including failures. */
    public BackupDtos.Record run(Destination dest, String source) {
        String id = UUID.randomUUID().toString();
        BackupHistory rec = new BackupHistory();
        rec.setId(id);
        rec.setSource(source);
        rec.setStatus("pending");
        rec.setStartedAt(Instant.now());
        history.saveAndFlush(rec);
        try {
            Uploaded up = dumpUploadVerify(dest, id);
            rec.setStatus("verified");
            rec.setS3Key(up.key());
            rec.setSizeBytes(up.size());
            rec.setSha256(up.sha256());
            rec.setCompletedAt(Instant.now());
            history.saveAndFlush(rec);
            retain(dest);
            return toDto(rec);
        } catch (RuntimeException | IOException e) {
            rec.setStatus("failed");
            rec.setError(e.getMessage());
            rec.setCompletedAt(Instant.now());
            history.saveAndFlush(rec);
            throw new ApiException(500, "backup_failed", "Backup failed: " + e.getMessage());
        }
    }

    private record Uploaded(String key, long size, String sha256) {}

    private Uploaded dumpUploadVerify(Destination dest, String id) throws IOException {
        Path tmp = Files.createTempDirectory("kinetirx-backup-");
        try {
            Path dump = tmp.resolve("db.dump");
            db.dumpTo(dump);
            long size = Files.size(dump);
            String sum = sha256(dump);
            String key = "kinetirx-backups/" + java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                    .withZone(java.time.ZoneOffset.UTC).format(Instant.now()) + "-" + id.substring(0, 8) + ".dump";
            try (S3Client s3 = s3(dest)) {
                s3.putObject(PutObjectRequest.builder().bucket(dest.bucket()).key(key).contentLength(size).build(),
                        RequestBody.fromFile(dump));
                // Verify by downloading the object back. HEAD alone can lie on some S3-compatible servers.
                Path verify = tmp.resolve("verify.dump");
                s3.getObject(GetObjectRequest.builder().bucket(dest.bucket()).key(key).build(),
                        ResponseTransformer.toFile(verify));
                if (!sum.equals(sha256(verify))) {
                    s3.deleteObject(DeleteObjectRequest.builder().bucket(dest.bucket()).key(key).build());
                    throw new IOException("uploaded backup failed verification: checksum mismatch");
                }
            }
            return new Uploaded(key, size, sum);
        } finally {
            deleteTree(tmp);
        }
    }

    private void retain(Destination dest) {
        List<BackupHistory> stale = history.verifiedBeyond(dest.retainCount());
        if (stale.isEmpty()) return;
        try (S3Client s3 = s3(dest)) {
            for (BackupHistory old : stale) {
                if (old.getS3Key() != null && !old.getS3Key().isEmpty()) {
                    s3.deleteObject(DeleteObjectRequest.builder().bucket(dest.bucket()).key(old.getS3Key()).build());
                }
                history.delete(old);
            }
        }
    }

    /** Restores the live database from a verified S3 backup, after checking its checksum. */
    public void restoreFromS3(Destination dest, BackupDtos.Record rec) {
        if (rec.s3Key() == null || rec.s3Key().isEmpty()) {
            throw new ApiException(500, "restore_failed", "Restore failed: backup record has no stored object key");
        }
        Path tmp = null;
        try {
            tmp = Files.createTempDirectory("kinetirx-restore-");
            Path dest0 = tmp.resolve("restore.dump");
            try (S3Client s3 = s3(dest)) {
                s3.getObject(GetObjectRequest.builder().bucket(dest.bucket()).key(rec.s3Key()).build(),
                        ResponseTransformer.toFile(dest0));
            }
            if (rec.sha256() != null && !rec.sha256().isEmpty() && !rec.sha256().equals(sha256(dest0))) {
                throw new IOException("downloaded backup failed checksum verification — refusing to restore");
            }
            db.restoreFrom(dest0);
        } catch (IOException | RuntimeException e) {
            throw new ApiException(500, "restore_failed", "Restore failed: " + e.getMessage());
        } finally {
            deleteTree(tmp);
        }
    }

    /** Restores from a dump file the operator uploaded. */
    public void restoreFromUpload(InputStream uploaded) {
        Path tmp = null;
        try {
            tmp = Files.createTempDirectory("kinetirx-restore-upload-");
            Path file = tmp.resolve("restore.dump");
            Files.copy(uploaded, file);
            db.restoreFrom(file);
        } catch (IOException | RuntimeException e) {
            throw new ApiException(500, "restore_failed", "Restore failed: " + e.getMessage());
        } finally {
            deleteTree(tmp);
        }
    }

    /** Streams a fresh dump straight to the caller, even when no S3 destination is configured. */
    public void streamDump(OutputStream out) throws IOException {
        Path tmp = Files.createTempDirectory("kinetirx-local-dump-");
        try {
            Path dump = tmp.resolve("db.dump");
            db.dumpTo(dump);
            Files.copy(dump, out);
        } finally {
            deleteTree(tmp);
        }
    }

    // ---- helpers ---------------------------------------------------------

    S3Client s3(Destination d) {
        return S3Client.builder()
                .region(Region.of(d.region()))
                .endpointOverride(URI.create(d.endpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(d.accessKeyId(), d.secretAccessKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    static String sha256(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[1 << 16];
            int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
            return HexFormat.of().formatHex(md.digest());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    private static void deleteTree(Path dir) {
        if (dir == null) return;
        try (var walk = Files.walk(dir)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
            // best-effort cleanup of temporary files
        }
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    static BackupDtos.Record toDto(BackupHistory h) {
        return new BackupDtos.Record(h.getId(), h.getSource(), h.getStatus(), h.getS3Key(), h.getSizeBytes(),
                h.getSha256(), h.getError(), h.getStartedAt(), h.getCompletedAt());
    }

}
