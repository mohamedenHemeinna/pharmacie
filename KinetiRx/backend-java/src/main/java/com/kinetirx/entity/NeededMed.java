package com.kinetirx.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/** needed_meds table. */
@Entity
@Table(name = "needed_meds")
@Getter
@Setter
@NoArgsConstructor
public class NeededMed {

    @Id @Column(name = "id") private String id;
    @Column(name = "patient_id") private String patientId;
    @Column(name = "med", nullable = false) private String med;
    @Column(name = "name", nullable = false) private String name;
    @Column(name = "phone", nullable = false) private String phone;
    @Column(name = "dist", nullable = false) private String dist;
    @Column(name = "time", nullable = false) private String time;
    @Column(name = "qty", nullable = false) private BigDecimal qty;
    @Column(name = "status", nullable = false) private String status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
