package com.kinetirx.controller;

import com.kinetirx.dto.OperationsDtos.Campaign;
import com.kinetirx.dto.OperationsDtos.CampaignInput;
import com.kinetirx.service.OperationsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** /api/marketing-campaigns. */
@RestController
@RequestMapping("/api/marketing-campaigns")
public class CampaignController {

    private final OperationsService service;

    public CampaignController(OperationsService service) {
        this.service = service;
    }

    @GetMapping
    public List<Campaign> list() {
        return service.listCampaigns();
    }

    @GetMapping("/{id}")
    public Campaign get(@PathVariable String id) {
        return service.getCampaign(id);
    }

    @PostMapping
    public ResponseEntity<Campaign> create(@RequestBody CampaignInput in) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createCampaign(in));
    }

    @PutMapping("/{id}")
    public Campaign update(@PathVariable String id, @RequestBody CampaignInput in) {
        return service.updateCampaign(id, in);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.deleteCampaign(id);
        return ResponseEntity.noContent().build();
    }
}
