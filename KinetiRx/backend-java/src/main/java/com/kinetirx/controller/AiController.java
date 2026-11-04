package com.kinetirx.controller;

import com.kinetirx.dto.AiDtos;
import com.kinetirx.service.AiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** /api/ai/ask (any employee), /api/ocr/parse-bill (inward-ocr), /api/ai-config (admin). */
@RestController
public class AiController {

    private final AiService service;

    public AiController(AiService service) {
        this.service = service;
    }

    @PostMapping("/api/ai/ask")
    public AiDtos.AskResponse ask(@RequestBody AiDtos.AskRequest req) {
        return service.ask(req);
    }

    @PostMapping("/api/ocr/parse-bill")
    public java.util.Map<String, Object> parseBill(@RequestBody AiDtos.ParseBillRequest req) {
        return service.parseBill(req);
    }

    @GetMapping("/api/ai-config")
    public AiDtos.ProviderStatus getConfig() {
        return service.status();
    }

    @PostMapping("/api/ai-config")
    public AiDtos.ProviderStatus saveConfig(@RequestBody AiDtos.ProviderInput in) {
        return service.save(in);
    }

    @DeleteMapping("/api/ai-config")
    public ResponseEntity<Void> deleteConfig() {
        service.delete();
        return ResponseEntity.noContent().build();
    }
}
