package com.kinetirx.controller;

import com.kinetirx.dto.StaffDtos;
import com.kinetirx.service.StaffService;
import org.springframework.web.bind.annotation.*;

/** /api/daily-register: singleton GET and PUT. */
@RestController
@RequestMapping("/api/daily-register")
public class DailyRegisterController {

    private final StaffService service;

    public DailyRegisterController(StaffService service) {
        this.service = service;
    }

    @GetMapping
    public StaffDtos.DailyRegister get() {
        return service.getRegister();
    }

    @PutMapping
    public StaffDtos.DailyRegister put(@RequestBody StaffDtos.DailyRegisterInput in) {
        return service.putRegister(in);
    }
}
