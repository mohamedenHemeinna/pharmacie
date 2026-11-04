package com.kinetirx.controller;

import com.kinetirx.dto.StaffDtos;
import com.kinetirx.security.EmployeePrincipal;
import com.kinetirx.service.StaffService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** /api/employees: list and get need employee-mgmt; create, update, delete are admin-only (SecurityConfig). */
@RestController
@RequestMapping("/api/employees")
public class StaffController {

    private final StaffService service;

    public StaffController(StaffService service) {
        this.service = service;
    }

    @GetMapping
    public List<StaffDtos.Employee> list() {
        return service.listEmployees();
    }

    @GetMapping("/{id}")
    public StaffDtos.Employee get(@PathVariable String id) {
        return service.getEmployee(id);
    }

    @PostMapping
    public ResponseEntity<StaffDtos.Employee> create(@RequestBody StaffDtos.EmployeeInput in) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createEmployee(in));
    }

    @PutMapping("/{id}")
    public StaffDtos.Employee update(@PathVariable String id, @RequestBody StaffDtos.EmployeeInput in) {
        return service.updateEmployee(id, in);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id,
                                       @AuthenticationPrincipal EmployeePrincipal caller) {
        service.deleteEmployee(id, caller.employeeId());
        return ResponseEntity.noContent().build();
    }
}
