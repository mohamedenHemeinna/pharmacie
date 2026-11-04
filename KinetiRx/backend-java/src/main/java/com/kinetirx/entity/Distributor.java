package com.kinetirx.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "distributors")
@Getter
@Setter
@NoArgsConstructor
public class Distributor {

    @Id @Column(name = "id") private String id;
    @Column(name = "name", nullable = false) private String name;
    @Column(name = "gstin", nullable = false) private String gstin;
    @Column(name = "phone", nullable = false) private String phone;
    @Column(name = "addr", nullable = false) private String addr;
    @Column(name = "dl_no") private String dlNo;
    @Column(name = "email") private String email;
    @Column(name = "contact_person") private String contactPerson;
    @Column(name = "registered_date") private String registeredDate;
    @Column(name = "source") private String source;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
