package com.kinetirx.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/** backup_config: singleton S3 destination (V5). secret_access_key_cipher is AES-GCM sealed. */
@Entity
@Table(name = "backup_config")
@Getter
@Setter
@NoArgsConstructor
public class BackupConfig {

    @Id @Column(name = "id") private Boolean id = Boolean.TRUE;
    @Column(name = "endpoint", nullable = false) private String endpoint;
    @Column(name = "bucket", nullable = false) private String bucket;
    @Column(name = "region", nullable = false) private String region;
    @Column(name = "access_key_id", nullable = false) private String accessKeyId;
    @Column(name = "secret_access_key_cipher", nullable = false) private byte[] secretAccessKeyCipher;
    @Column(name = "interval_days", nullable = false) private int intervalDays;
    @Column(name = "retain_count", nullable = false) private int retainCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
