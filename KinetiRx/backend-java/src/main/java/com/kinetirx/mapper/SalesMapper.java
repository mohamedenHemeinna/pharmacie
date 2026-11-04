package com.kinetirx.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.kinetirx.common.JsonCodec;
import com.kinetirx.dto.SalesDtos;
import com.kinetirx.entity.SalesRecord;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class SalesMapper {

    private static final TypeReference<List<SalesDtos.ItemDetail>> ITEMS = new TypeReference<>() {};

    private final JsonCodec json;

    public SalesMapper(JsonCodec json) {
        this.json = json;
    }

    public SalesDtos.Sale toDto(SalesRecord s) {
        return new SalesDtos.Sale(
                s.getId(), s.getInv(), s.getInvoiceNo(), s.getDate(), s.getCust(), s.getName(),
                s.getPatient(), s.getPatientId(), s.getPhone(), s.getItems(), s.getQty(),
                opt(s.getAmt()), opt(s.getTotal()), s.getMode(),
                json.read(s.getItemsDetail(), ITEMS, List.of()),
                opt(s.getSubtotal()), opt(s.getDiscountPercent()), s.getDoctor(), s.getAddress(),
                s.getAgeGender(), opt(s.getPaidAmount()), opt(s.getDueAmount()),
                s.getCreatedAt(), s.getUpdatedAt());
    }

    public void apply(SalesRecord s, SalesDtos.SaleInput in) {
        s.setInv(in.inv());
        s.setInvoiceNo(in.invoiceNo());
        s.setDate(in.date());
        s.setCust(in.cust());
        s.setName(in.name());
        s.setPatient(in.patient());
        s.setPatientId(in.patientId());
        s.setPhone(in.phone());
        s.setItems(in.items());
        s.setQty(in.qty());
        s.setAmt(bd(in.amt()));
        s.setTotal(bd(in.total()));
        s.setMode(in.mode());
        s.setItemsDetail(json.write(in.itemsDetail() == null ? List.of() : in.itemsDetail()));
        s.setSubtotal(bd(in.subtotal()));
        s.setDiscountPercent(in.discountPercent() == null ? null
                : BigDecimal.valueOf(in.discountPercent()).setScale(2, RoundingMode.HALF_UP));
        s.setDoctor(in.doctor());
        s.setAddress(in.address());
        s.setAgeGender(in.ageGender());
        s.setPaidAmount(bd(in.paidAmount()));
        s.setDueAmount(bd(in.dueAmount()));
    }

    private static Double opt(BigDecimal v) {
        return v == null ? null : v.doubleValue();
    }

    private static BigDecimal bd(Double v) {
        return v == null ? null : BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }
}
