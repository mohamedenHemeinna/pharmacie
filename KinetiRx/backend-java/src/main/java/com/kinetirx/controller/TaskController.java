package com.kinetirx.controller;

import com.kinetirx.dto.OperationsDtos.Task;
import com.kinetirx.dto.OperationsDtos.TaskInput;
import com.kinetirx.service.OperationsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** /api/worksheet-tasks. */
@RestController
@RequestMapping("/api/worksheet-tasks")
public class TaskController {

    private final OperationsService service;

    public TaskController(OperationsService service) {
        this.service = service;
    }

    @GetMapping
    public List<Task> list() {
        return service.listTasks();
    }

    @GetMapping("/{id}")
    public Task get(@PathVariable String id) {
        return service.getTask(id);
    }

    @PostMapping
    public ResponseEntity<Task> create(@RequestBody TaskInput in) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createTask(in));
    }

    @PutMapping("/{id}")
    public Task update(@PathVariable String id, @RequestBody TaskInput in) {
        return service.updateTask(id, in);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
