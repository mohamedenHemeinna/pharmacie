package com.kinetirx.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/** worksheet_tasks table. */
@Entity
@Table(name = "worksheet_tasks")
@Getter
@Setter
@NoArgsConstructor
public class WorksheetTask {

    @Id @Column(name = "id") private String id;
    @Column(name = "cat", nullable = false) private String cat;
    @Column(name = "description", nullable = false) private String description;
    @Column(name = "date", nullable = false) private String date;
    @Column(name = "status", nullable = false) private String status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
