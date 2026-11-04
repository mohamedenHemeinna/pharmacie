package com.kinetirx.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/** Wire shapes for medicines, medicine groups, and distributors. Field names match Go JSON exactly. */
public final class InventoryDtos {

    private InventoryDtos() {}

    /** Request body for create and update. Numeric fields default to zero, as in Go. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MedicineInput(
            String id,
            String name,
            String company,
            String dist,
            String distributor,
            String hsn,
            String batch,
            String pack,
            String salt,
            String generic,
            @JsonProperty("group") String group,
            String rack,
            double stock,
            double rate,
            double omrp,
            double mrp,
            String scheme,
            double gst,
            double disc,
            double tabsPerStrip,
            String expiry,
            boolean isLabTest,
            Boolean trackStock,
            String itemType,
            String barcode) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Medicine(
            String id,
            String name,
            String company,
            String dist,
            String distributor,
            String hsn,
            String batch,
            String pack,
            String salt,
            String generic,
            @JsonProperty("group") String group,
            String rack,
            double stock,
            double rate,
            double omrp,
            double mrp,
            String scheme,
            double gst,
            double disc,
            double tabsPerStrip,
            String expiry,
            boolean isLabTest,
            boolean trackStock,
            String itemType,
            String barcode,
            Instant createdAt,
            Instant updatedAt) {}

    public record MedicineGroupInput(String id, String name) {}

    public record MedicineGroup(String id, String name, Instant createdAt, Instant updatedAt) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DistributorInput(
            String id,
            String name,
            String gstin,
            String phone,
            String addr,
            String dlNo,
            String email,
            String contactPerson,
            String registeredDate,
            String source) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Distributor(
            String id,
            String name,
            String gstin,
            String phone,
            String addr,
            String dlNo,
            String email,
            String contactPerson,
            String registeredDate,
            String source,
            Instant createdAt,
            Instant updatedAt) {}
}
