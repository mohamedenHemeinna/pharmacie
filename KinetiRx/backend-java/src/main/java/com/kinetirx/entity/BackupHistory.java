package com.kinetirx.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** backup_history: one row per attempt (V5). A backup counts only once status is verified. */
@Entity
@Table(name = "backup_history")
@Getter
@Setter
@NoArgsConstructor
public class BackupHistory {

    @Id @Column(name = "id") private String id;
    @Column(name = "source", nullable = false) private String source;
    @Column(name = "status", nullable = false) private String status;
    @Column(name = "s3_key") private String s3Key;
    @Column(name = "size_bytes") private Long sizeBytes;
    @Column(name = "sha256") private String sha256;
    @Column(name = "error") private String error;
    @Column(name = "started_at", nullable = false) private Instant startedAt;
    @Column(name = "completed_at") private Instant completedAt;
}
