package com.kinetirx.controller;

import com.kinetirx.dto.OperationsDtos.OpdInput;
import com.kinetirx.dto.OperationsDtos.OpdVisit;
import com.kinetirx.service.OperationsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** /api/opd-visits. */
@RestController
@RequestMapping("/api/opd-visits")
public class OpdController {

    private final OperationsService service;

    public OpdController(OperationsService service) {
        this.service = service;
    }

    @GetMapping
    public List<OpdVisit> list() {
        return service.listOpd();
    }

    @GetMapping("/{id}")
    public OpdVisit get(@PathVariable String id) {
        return service.getOpd(id);
    }

    @PostMapping
    public ResponseEntity<OpdVisit> create(@RequestBody OpdInput in) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createOpd(in));
    }

    @PutMapping("/{id}")
    public OpdVisit update(@PathVariable String id, @RequestBody OpdInput in) {
        return service.updateOpd(id, in);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.deleteOpd(id);
        return ResponseEntity.noContent().build();
    }
}
