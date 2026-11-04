package com.kinetirx.controller;

import com.kinetirx.dto.StaffDtos;
import com.kinetirx.service.StaffService;
import org.springframework.web.bind.annotation.*;

/** /api/invoice-config: singleton GET (any employee) and PUT (invoice-settings). */
@RestController
@RequestMapping("/api/invoice-config")
public class InvoiceConfigController {

    private final StaffService service;

    public InvoiceConfigController(StaffService service) {
        this.service = service;
    }

    @GetMapping
    public StaffDtos.InvoiceConfig get() {
        return service.getInvoiceConfig();
    }

    @PutMapping
    public StaffDtos.InvoiceConfig put(@RequestBody StaffDtos.InvoiceConfigInput in) {
        return service.putInvoiceConfig(in);
    }
}
