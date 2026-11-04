package com.kinetirx.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/** invoice_config: singleton row (id is always true). */
@Entity
@Table(name = "invoice_config")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceConfig {

    @Id @Column(name = "id") private Boolean id = Boolean.TRUE;
    @Column(name = "name", nullable = false) private String name = "";
    @Column(name = "store_name") private String storeName;
    @Column(name = "subtitle") private String subtitle;
    @Column(name = "dl", nullable = false) private String dl = "";
    @Column(name = "gst", nullable = false) private String gst = "";
    @Column(name = "phone", nullable = false) private String phone = "";
    @Column(name = "wa_group", nullable = false) private String waGroup = "";
    @Column(name = "addr", nullable = false) private String addr = "";
    @Column(name = "terms", nullable = false) private String terms = "";
    @Column(name = "logo_url") private String logoUrl;
    @Column(name = "retention_months") private Integer retentionMonths;
    @Column(name = "retention_policy_notice") private String retentionPolicyNotice;
    @Column(name = "auto_purge_old_invoices", nullable = false) private boolean autoPurgeOldInvoices = true;
    @Column(name = "last_purge_date") private String lastPurgeDate;
    @Column(name = "director") private String director;
    @Column(name = "pharmacist") private String pharmacist;
    @Column(name = "currency") private String currency;
    @Column(name = "printer_type") private String printerType;
    @Column(name = "header_theme") private String headerTheme;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
