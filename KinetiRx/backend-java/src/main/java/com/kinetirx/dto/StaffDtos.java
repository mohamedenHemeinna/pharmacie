package com.kinetirx.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Employees, daily register, invoice config, and Master PIN wire shapes (same names as Go JSON). */
public final class StaffDtos {

    private StaffDtos() {}

    public record EmployeeInput(String id, String name, String desig, String password, String phone,
                                String role, String pin, List<String> permissions) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Employee(String id, String name, String desig, String phone, String role,
                           List<String> permissions, boolean mustChangePassword,
                           Instant createdAt, Instant updatedAt) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DailyRegister(String date, double prevBD, double todaySell, double phonePe, double expenses,
                                double bankShift, boolean isLocked, double openingCash, double totalSales,
                                double cashSales, double upiSales, double cardSales, double totalExpenses,
                                Map<String, Integer> denominations, double closingPhysicalCash,
                                double cashDifference, boolean isDrawerClosed, Instant updatedAt) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DailyRegisterInput(String date, double prevBD, double todaySell, double phonePe, double expenses,
                                     double bankShift, boolean isLocked, double openingCash, double totalSales,
                                     double cashSales, double upiSales, double cardSales, double totalExpenses,
                                     Map<String, Integer> denominations, double closingPhysicalCash,
                                     double cashDifference, boolean isDrawerClosed) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InvoiceConfigInput(String name, String storeName, String subtitle, String dl, String gst,
                                     String phone, String waGroup, String addr, String terms, String logoUrl,
                                     Integer retentionMonths, String retentionPolicyNotice,
                                     boolean autoPurgeOldInvoices, String lastPurgeDate, String director,
                                     String pharmacist, String currency, String printerType, String headerTheme) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InvoiceConfig(String name, String storeName, String subtitle, String dl, String gst,
                                String phone, String waGroup, String addr, String terms, String logoUrl,
                                Integer retentionMonths, String retentionPolicyNotice,
                                boolean autoPurgeOldInvoices, String lastPurgeDate, String director,
                                String pharmacist, String currency, String printerType, String headerTheme,
                                Instant updatedAt) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MasterPinStatus(boolean isSet, String lockedUntil) {}

    public record SetMasterPinRequest(String currentPin, String newPin) {}

    public record VerifyMasterPinRequest(String pin) {}

    public record VerifyMasterPinResponse(boolean valid) {}
}
