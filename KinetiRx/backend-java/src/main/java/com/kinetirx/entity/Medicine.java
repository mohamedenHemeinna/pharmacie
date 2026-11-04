package com.kinetirx.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/** medicines table (V1, V8). Column names unchanged; "group" JSON field maps to group_name. */
@Entity
@Table(name = "medicines")
@Getter
@Setter
@NoArgsConstructor
public class Medicine {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "name", nullable = false) private String name;
    @Column(name = "company", nullable = false) private String company;
    @Column(name = "dist", nullable = false) private String dist;

    /** FK to distributors.id (ON DELETE SET NULL in the database). Kept as a plain column. */
    @Column(name = "distributor_id")
    private String distributorId;

    @Column(name = "hsn", nullable = false) private String hsn;
    @Column(name = "batch", nullable = false) private String batch;
    @Column(name = "pack", nullable = false) private String pack;
    @Column(name = "salt", nullable = false) private String salt;
    @Column(name = "generic") private String generic;
    @Column(name = "group_name", nullable = false) private String groupName;
    @Column(name = "rack", nullable = false) private String rack;
    @Column(name = "stock", nullable = false) private BigDecimal stock;
    @Column(name = "rate", nullable = false) private BigDecimal rate;
    @Column(name = "omrp", nullable = false) private BigDecimal omrp;
    @Column(name = "mrp", nullable = false) private BigDecimal mrp;
    @Column(name = "scheme", nullable = false) private String scheme;
    @Column(name = "gst", nullable = false) private BigDecimal gst;
    @Column(name = "disc", nullable = false) private BigDecimal disc;
    @Column(name = "tabs_per_strip", nullable = false) private BigDecimal tabsPerStrip;
    @Column(name = "expiry", nullable = false) private String expiry;
    @Column(name = "is_lab_test", nullable = false) private boolean labTest;
    @Column(name = "track_stock", nullable = false) private boolean trackStock;
    @Column(name = "item_type", nullable = false) private String itemType;
    @Column(name = "barcode") private String barcode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
