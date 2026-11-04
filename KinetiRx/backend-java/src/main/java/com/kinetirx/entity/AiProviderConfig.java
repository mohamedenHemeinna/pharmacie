package com.kinetirx.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/** ai_provider_config: singleton (V11). api_key_cipher is AES-GCM sealed; never returned by any endpoint. */
@Entity
@Table(name = "ai_provider_config")
@Getter
@Setter
@NoArgsConstructor
public class AiProviderConfig {

    @Id @Column(name = "id") private Boolean id = Boolean.TRUE;
    @Column(name = "base_url", nullable = false) private String baseUrl;
    @Column(name = "model", nullable = false) private String model;
    @Column(name = "api_key_cipher", nullable = false) private byte[] apiKeyCipher;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
