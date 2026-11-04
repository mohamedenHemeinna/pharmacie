package com.kinetirx.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/** Patients and due-khata wire shapes. Pointer fields in Go are omitempty, mirrored by NON_NULL. */
public final class PatientDtos {

    private PatientDtos() {}

    public record PurchaseHistoryEntry(String date, String items, double amount) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PatientInput(
            String id, String name, String phone, String age, String gender, String ageGender,
            String addr, String address, String doc, String doctor, String reason,
            double totalDue, Double dueAmount, String lastDate, String lastVisitDate, Integer totalVisits,
            List<PurchaseHistoryEntry> purchaseHistory, List<String> bloodTests) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Patient(
            String id, String name, String phone, String age, String gender, String ageGender,
            String addr, String address, String doc, String doctor, String reason,
            double totalDue, Double dueAmount, String lastDate, String lastVisitDate, Integer totalVisits,
            List<PurchaseHistoryEntry> purchaseHistory, List<String> bloodTests,
            Instant createdAt, Instant updatedAt) {}

    public record NextId(String id) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DueInput(
            String id, String patientId, String name, String phone, String addr, String doc,
            String reason, double due, String lastDate) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Due(
            String id, String patientId, String name, String phone, String addr, String doc,
            String reason, double due, String lastDate, Instant createdAt, Instant updatedAt) {}
}
