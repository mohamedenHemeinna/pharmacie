package com.kinetirx.controller;

import com.kinetirx.dto.OperationsDtos.Expense;
import com.kinetirx.dto.OperationsDtos.ExpenseInput;
import com.kinetirx.service.OperationsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** /api/expenses. Permission rules live in SecurityConfig. */
@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final OperationsService service;

    public ExpenseController(OperationsService service) {
        this.service = service;
    }

    @GetMapping
    public List<Expense> list(@RequestParam(required = false) String date) {
        return service.listExpenses(date);
    }

    @GetMapping("/{id}")
    public Expense get(@PathVariable String id) {
        return service.getExpense(id);
    }

    @PostMapping
    public ResponseEntity<Expense> create(@RequestBody ExpenseInput in) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createExpense(in));
    }

    @PutMapping("/{id}")
    public Expense update(@PathVariable String id, @RequestBody ExpenseInput in) {
        return service.updateExpense(id, in);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }
}
