package com.kinetirx.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/** patients_due: due-khata credit ledger (V1). */
@Entity
@Table(name = "patients_due")
@Getter
@Setter
@NoArgsConstructor
public class PatientDue {

    @Id @Column(name = "id") private String id;
    @Column(name = "patient_id") private String patientId;
    @Column(name = "name", nullable = false) private String name;
    @Column(name = "phone", nullable = false) private String phone;
    @Column(name = "addr", nullable = false) private String addr;
    @Column(name = "doc", nullable = false) private String doc;
    @Column(name = "reason", nullable = false) private String reason;
    @Column(name = "due", nullable = false) private BigDecimal due;
    @Column(name = "last_date", nullable = false) private String lastDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
