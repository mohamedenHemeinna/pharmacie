package com.kinetirx.controller;

import com.kinetirx.dto.SalesDtos;
import com.kinetirx.service.SalesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** /api/sales (list, get, create only). */
@RestController
@RequestMapping("/api/sales")
public class SalesController {

    private final SalesService service;

    public SalesController(SalesService service) {
        this.service = service;
    }

    @GetMapping
    public List<SalesDtos.Sale> list(@RequestParam(required = false) String date,
                                     @RequestParam(required = false) String patientId) {
        return service.search(date, patientId);
    }

    @GetMapping("/{id}")
    public SalesDtos.Sale get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<SalesDtos.Sale> create(@RequestBody SalesDtos.SaleInput in) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(in));
    }
}
