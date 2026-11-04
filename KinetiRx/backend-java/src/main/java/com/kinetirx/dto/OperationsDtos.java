package com.kinetirx.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/** Expenses, medicine orders, OPD visits, marketing campaigns, and worksheet tasks. */
public final class OperationsDtos {

    private OperationsDtos() {}

    public record ExpenseInput(String id, String date, String cat, String desc, double amt) {}

    public record Expense(String id, String date, String cat, String desc, double amt,
                          Instant createdAt, Instant updatedAt) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record NeededMedInput(String id, String patientId, String med, String name, String phone,
                                 String dist, String time, double qty, String status) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record NeededMed(String id, String patientId, String med, String name, String phone,
                            String dist, String time, double qty, String status,
                            Instant createdAt, Instant updatedAt) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OpdInput(String id, String patientId, String name, String phone, String ageSex,
                           String doc, String vdate, String rvdate, String btest, String reminder) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OpdVisit(String id, String patientId, String name, String phone, String ageSex,
                           String doc, String vdate, String rvdate, String btest, String reminder,
                           Instant createdAt, Instant updatedAt) {}

    public record CampaignInput(String id, String doc, String date, String action, String status) {}

    public record Campaign(String id, String doc, String date, String action, String status,
                           Instant createdAt, Instant updatedAt) {}

    public record TaskInput(String id, String cat, String desc, String date, String status) {}

    public record Task(String id, String cat, String desc, String date, String status,
                       Instant createdAt, Instant updatedAt) {}
}
