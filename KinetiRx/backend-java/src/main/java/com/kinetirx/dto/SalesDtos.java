package com.kinetirx.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/** POS sales wire shapes. Pointer fields in Go are omitempty, mirrored by NON_NULL. */
public final class SalesDtos {

    private SalesDtos() {}

    public record ItemDetail(String name, double qty, double price, double total) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SaleInput(
            String id, String inv, String invoiceNo, String date, String cust, String name,
            String patient, String patientId, String phone, String items, String qty,
            Double amt, Double total, String mode, List<ItemDetail> itemsDetail,
            Double subtotal, Double discountPercent, String doctor, String address, String ageGender,
            Double paidAmount, Double dueAmount) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Sale(
            String id, String inv, String invoiceNo, String date, String cust, String name,
            String patient, String patientId, String phone, String items, String qty,
            Double amt, Double total, String mode, List<ItemDetail> itemsDetail,
            Double subtotal, Double discountPercent, String doctor, String address, String ageGender,
            Double paidAmount, Double dueAmount, Instant createdAt, Instant updatedAt) {}
}
