package com.kinetirx.controller;

import com.kinetirx.dto.OperationsDtos.NeededMed;
import com.kinetirx.dto.OperationsDtos.NeededMedInput;
import com.kinetirx.service.OperationsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** /api/needed-meds. */
@RestController
@RequestMapping("/api/needed-meds")
public class NeededMedController {

    private final OperationsService service;

    public NeededMedController(OperationsService service) {
        this.service = service;
    }

    @GetMapping
    public List<NeededMed> list() {
        return service.listNeededMeds();
    }

    @GetMapping("/{id}")
    public NeededMed get(@PathVariable String id) {
        return service.getNeededMed(id);
    }

    @PostMapping
    public ResponseEntity<NeededMed> create(@RequestBody NeededMedInput in) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createNeededMed(in));
    }

    @PutMapping("/{id}")
    public NeededMed update(@PathVariable String id, @RequestBody NeededMedInput in) {
        return service.updateNeededMed(id, in);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.deleteNeededMed(id);
        return ResponseEntity.noContent().build();
    }
}
