package com.kinetirx.controller;

import com.kinetirx.dto.PatientDtos;
import com.kinetirx.service.PatientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** /api/patients and /api/due-khata. Permission rules live in SecurityConfig. */
@RestController
public class PatientController {

    private final PatientService service;

    public PatientController(PatientService service) {
        this.service = service;
    }

    @GetMapping("/api/patients")
    public List<PatientDtos.Patient> list() {
        return service.list();
    }

    @GetMapping("/api/patients/next-id")
    public PatientDtos.NextId nextId() {
        return service.nextId();
    }

    @GetMapping("/api/patients/{id}")
    public PatientDtos.Patient get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping("/api/patients")
    public ResponseEntity<PatientDtos.Patient> create(@RequestBody PatientDtos.PatientInput in) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(in));
    }

    @PutMapping("/api/patients/{id}")
    public PatientDtos.Patient update(@PathVariable String id, @RequestBody PatientDtos.PatientInput in) {
        return service.update(id, in);
    }

    @DeleteMapping("/api/patients/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/due-khata")
    public List<PatientDtos.Due> listDues() {
        return service.listDues();
    }

    @GetMapping("/api/due-khata/{id}")
    public PatientDtos.Due getDue(@PathVariable String id) {
        return service.getDue(id);
    }

    @PostMapping("/api/due-khata")
    public ResponseEntity<PatientDtos.Due> createDue(@RequestBody PatientDtos.DueInput in) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createDue(in));
    }

    @PutMapping("/api/due-khata/{id}")
    public PatientDtos.Due updateDue(@PathVariable String id, @RequestBody PatientDtos.DueInput in) {
        return service.updateDue(id, in);
    }

    @DeleteMapping("/api/due-khata/{id}")
    public ResponseEntity<Void> deleteDue(@PathVariable String id) {
        service.deleteDue(id);
        return ResponseEntity.noContent().build();
    }
}
