package com.kinetirx.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/** opd_visits table. */
@Entity
@Table(name = "opd_visits")
@Getter
@Setter
@NoArgsConstructor
public class OpdVisit {

    @Id @Column(name = "id") private String id;
    @Column(name = "patient_id") private String patientId;
    @Column(name = "name", nullable = false) private String name;
    @Column(name = "phone", nullable = false) private String phone;
    @Column(name = "age_sex", nullable = false) private String ageSex;
    @Column(name = "doc", nullable = false) private String doc;
    @Column(name = "vdate", nullable = false) private String vdate;
    @Column(name = "rvdate", nullable = false) private String rvdate;
    @Column(name = "btest", nullable = false) private String btest;
    @Column(name = "reminder", nullable = false) private String reminder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
