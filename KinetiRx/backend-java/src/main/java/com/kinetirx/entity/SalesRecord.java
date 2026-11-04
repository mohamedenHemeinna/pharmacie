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

/** sales_history: POS invoices (V1). Append-only through the API. */
@Entity
@Table(name = "sales_history")
@Getter
@Setter
@NoArgsConstructor
public class SalesRecord {

    @Id @Column(name = "id") private String id;
    @Column(name = "inv") private String inv;
    @Column(name = "invoice_no") private String invoiceNo;
    @Column(name = "date", nullable = false) private String date;
    @Column(name = "cust") private String cust;
    @Column(name = "name") private String name;
    @Column(name = "patient") private String patient;
    @Column(name = "patient_id") private String patientId;
    @Column(name = "phone") private String phone;
    @Column(name = "items") private String items;
    @Column(name = "qty") private String qty;
    @Column(name = "amt") private BigDecimal amt;
    @Column(name = "total") private BigDecimal total;
    @Column(name = "mode", nullable = false) private String mode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items_detail", nullable = false, columnDefinition = "jsonb")
    private String itemsDetail = "[]";

    @Column(name = "subtotal") private BigDecimal subtotal;
    @Column(name = "discount_percent") private BigDecimal discountPercent;
    @Column(name = "doctor") private String doctor;
    @Column(name = "address") private String address;
    @Column(name = "age_gender") private String ageGender;
    @Column(name = "paid_amount") private BigDecimal paidAmount;
    @Column(name = "due_amount") private BigDecimal dueAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
