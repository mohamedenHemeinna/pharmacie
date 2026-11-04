package com.kinetirx.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/** marketing_campaigns table. */
@Entity
@Table(name = "marketing_campaigns")
@Getter
@Setter
@NoArgsConstructor
public class MarketingCampaign {

    @Id @Column(name = "id") private String id;
    @Column(name = "doc", nullable = false) private String doc;
    @Column(name = "date", nullable = false) private String date;
    @Column(name = "action", nullable = false) private String action;
    @Column(name = "status", nullable = false) private String status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
