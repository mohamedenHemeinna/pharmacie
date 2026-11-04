package com.kinetirx.controller;

import com.kinetirx.dto.StaffDtos.MasterPinStatus;
import com.kinetirx.dto.StaffDtos.SetMasterPinRequest;
import com.kinetirx.dto.StaffDtos.VerifyMasterPinRequest;
import com.kinetirx.dto.StaffDtos.VerifyMasterPinResponse;
import com.kinetirx.service.MasterPinService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/** /api/security/master-pin. Verify is also rate-limited per IP, as in the Go router. */
@RestController
@RequestMapping("/api/security/master-pin")
public class MasterPinController {

    private final MasterPinService service;
    private final com.kinetirx.common.RateLimiter rateLimiter;

    public MasterPinController(MasterPinService service, com.kinetirx.common.RateLimiter rateLimiter) {
        this.service = service;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping
    public MasterPinStatus status() {
        return service.status();
    }

    @PutMapping
    public MasterPinStatus set(@RequestBody SetMasterPinRequest req) {
        return service.set(req.currentPin(), req.newPin());
    }

    @PostMapping("/verify")
    public VerifyMasterPinResponse verify(@RequestBody VerifyMasterPinRequest req, HttpServletRequest http) {
        rateLimiter.check("master-pin-verify", http, 10, Duration.ofMinutes(1));
        return service.verify(req.pin());
    }
}
