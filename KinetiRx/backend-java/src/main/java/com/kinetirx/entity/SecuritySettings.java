package com.kinetirx.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/** security_settings: singleton holding the org-wide Master Security PIN hash and lockout state (V4). */
@Entity
@Table(name = "security_settings")
@Getter
@Setter
@NoArgsConstructor
public class SecuritySettings {

    @Id @Column(name = "id") private Boolean id = Boolean.TRUE;
    @Column(name = "master_pin_hash") private String masterPinHash;
    @Column(name = "failed_attempts", nullable = false) private int failedAttempts;
    @Column(name = "locked_until") private Instant lockedUntil;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
