package com.kinetirx.service;

import com.kinetirx.common.EventBus;
import com.kinetirx.dto.SalesDtos;
import com.kinetirx.entity.SalesRecord;
import com.kinetirx.exception.ApiException;
import com.kinetirx.mapper.SalesMapper;
import com.kinetirx.repository.SalesRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** POS sales. Append-only: no update or delete, as in the Go service. */
@Service
public class SalesService {

    private final SalesRepository sales;
    private final SalesMapper mapper;
    private final EventBus events;

    public SalesService(SalesRepository sales, SalesMapper mapper, EventBus events) {
        this.sales = sales;
        this.mapper = mapper;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<SalesDtos.Sale> search(String date, String patientId) {
        return sales.search(nz(date), nz(patientId)).stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public SalesDtos.Sale get(String id) {
        return sales.findById(id).map(mapper::toDto).orElseThrow(() -> ApiException.notFound("Sale not found"));
    }

    @Transactional
    public SalesDtos.Sale create(SalesDtos.SaleInput in) {
        if (in.date() == null || in.date().isBlank()) throw ApiException.badRequest("Invalid sale payload: date is required");
        if (in.mode() == null || in.mode().isBlank()) throw ApiException.badRequest("Invalid sale payload: mode is required");
        SalesRecord s = new SalesRecord();
        s.setId(in.id() == null || in.id().isBlank() ? UUID.randomUUID().toString() : in.id());
        mapper.apply(s, in);
        try {
            SalesDtos.Sale out = mapper.toDto(sales.saveAndFlush(s));
            events.publish("sales");
            return out;
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("Failed to create sale (id may already exist, or patientId does not reference an existing patient)");
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
