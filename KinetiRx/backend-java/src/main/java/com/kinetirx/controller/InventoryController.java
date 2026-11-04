package com.kinetirx.controller;

import com.kinetirx.dto.InventoryDtos;
import com.kinetirx.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** /api/medicines, /api/medicine-groups, /api/distributors. Permission rules live in SecurityConfig. */
@RestController
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping("/api/medicines")
    public List<InventoryDtos.Medicine> listMedicines() {
        return service.listMedicines();
    }

    @GetMapping("/api/medicines/{id}")
    public InventoryDtos.Medicine getMedicine(@PathVariable String id) {
        return service.getMedicine(id);
    }

    @PostMapping("/api/medicines")
    public ResponseEntity<InventoryDtos.Medicine> createMedicine(@RequestBody InventoryDtos.MedicineInput in) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createMedicine(in));
    }

    @PutMapping("/api/medicines/{id}")
    public InventoryDtos.Medicine updateMedicine(@PathVariable String id, @RequestBody InventoryDtos.MedicineInput in) {
        return service.updateMedicine(id, in);
    }

    @DeleteMapping("/api/medicines/{id}")
    public ResponseEntity<Void> deleteMedicine(@PathVariable String id) {
        service.deleteMedicine(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/medicine-groups")
    public List<InventoryDtos.MedicineGroup> listGroups() {
        return service.listGroups();
    }

    @GetMapping("/api/medicine-groups/{id}")
    public InventoryDtos.MedicineGroup getGroup(@PathVariable String id) {
        return service.getGroup(id);
    }

    @PostMapping("/api/medicine-groups")
    public ResponseEntity<InventoryDtos.MedicineGroup> createGroup(@RequestBody InventoryDtos.MedicineGroupInput in) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createGroup(in));
    }

    @PutMapping("/api/medicine-groups/{id}")
    public InventoryDtos.MedicineGroup updateGroup(@PathVariable String id, @RequestBody InventoryDtos.MedicineGroupInput in) {
        return service.updateGroup(id, in);
    }

    @DeleteMapping("/api/medicine-groups/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable String id) {
        service.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/distributors")
    public List<InventoryDtos.Distributor> listDistributors() {
        return service.listDistributors();
    }

    @GetMapping("/api/distributors/{id}")
    public InventoryDtos.Distributor getDistributor(@PathVariable String id) {
        return service.getDistributor(id);
    }

    @PostMapping("/api/distributors")
    public ResponseEntity<InventoryDtos.Distributor> createDistributor(@RequestBody InventoryDtos.DistributorInput in) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createDistributor(in));
    }

    @PutMapping("/api/distributors/{id}")
    public InventoryDtos.Distributor updateDistributor(@PathVariable String id, @RequestBody InventoryDtos.DistributorInput in) {
        return service.updateDistributor(id, in);
    }

    @DeleteMapping("/api/distributors/{id}")
    public ResponseEntity<Void> deleteDistributor(@PathVariable String id) {
        service.deleteDistributor(id);
        return ResponseEntity.noContent().build();
    }
}
