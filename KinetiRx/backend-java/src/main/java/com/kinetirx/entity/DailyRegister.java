package com.kinetirx.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/** daily_register: singleton row (id is always true). */
@Entity
@Table(name = "daily_register")
@Getter
@Setter
@NoArgsConstructor
public class DailyRegister {

    @Id @Column(name = "id") private Boolean id = Boolean.TRUE;
    @Column(name = "date") private String date;
    @Column(name = "prev_bd", nullable = false) private BigDecimal prevBd = BigDecimal.ZERO;
    @Column(name = "today_sell", nullable = false) private BigDecimal todaySell = BigDecimal.ZERO;
    @Column(name = "phone_pe", nullable = false) private BigDecimal phonePe = BigDecimal.ZERO;
    @Column(name = "expenses", nullable = false) private BigDecimal expenses = BigDecimal.ZERO;
    @Column(name = "bank_shift", nullable = false) private BigDecimal bankShift = BigDecimal.ZERO;
    @Column(name = "is_locked", nullable = false) private boolean locked;
    @Column(name = "opening_cash", nullable = false) private BigDecimal openingCash = BigDecimal.ZERO;
    @Column(name = "total_sales", nullable = false) private BigDecimal totalSales = BigDecimal.ZERO;
    @Column(name = "cash_sales", nullable = false) private BigDecimal cashSales = BigDecimal.ZERO;
    @Column(name = "upi_sales", nullable = false) private BigDecimal upiSales = BigDecimal.ZERO;
    @Column(name = "card_sales", nullable = false) private BigDecimal cardSales = BigDecimal.ZERO;
    @Column(name = "total_expenses", nullable = false) private BigDecimal totalExpenses = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "denominations", nullable = false, columnDefinition = "jsonb")
    private String denominations = "{}";

    @Column(name = "closing_physical_cash", nullable = false) private BigDecimal closingPhysicalCash = BigDecimal.ZERO;
    @Column(name = "cash_difference", nullable = false) private BigDecimal cashDifference = BigDecimal.ZERO;
    @Column(name = "is_drawer_closed", nullable = false) private boolean drawerClosed;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
