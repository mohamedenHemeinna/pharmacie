package com.kinetirx.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/** patients table (V1). purchase_history is JSONB; blood_tests is text[]. */
@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
public class Patient {

    @Id @Column(name = "id") private String id;
    @Column(name = "name", nullable = false) private String name;
    @Column(name = "phone", nullable = false) private String phone;
    @Column(name = "age") private String age;
    @Column(name = "gender") private String gender;
    @Column(name = "age_gender") private String ageGender;
    @Column(name = "addr") private String addr;
    @Column(name = "address") private String address;
    @Column(name = "doc") private String doc;
    @Column(name = "doctor") private String doctor;
    @Column(name = "reason") private String reason;
    @Column(name = "total_due", nullable = false) private BigDecimal totalDue;
    @Column(name = "due_amount") private BigDecimal dueAmount;
    @Column(name = "last_date") private String lastDate;
    @Column(name = "last_visit_date") private String lastVisitDate;
    @Column(name = "total_visits") private Integer totalVisits;

    /** JSON array of {date, items, amount}. Stored and read as JSON text. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "purchase_history", nullable = false, columnDefinition = "jsonb")
    private String purchaseHistory = "[]";

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "blood_tests", nullable = false, columnDefinition = "text[]")
    private String[] bloodTests = new String[0];

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
