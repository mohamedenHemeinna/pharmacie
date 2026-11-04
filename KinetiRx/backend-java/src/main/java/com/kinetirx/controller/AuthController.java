package com.kinetirx.controller;

import com.kinetirx.common.RateLimiter;
import com.kinetirx.dto.AuthDtos.ChangePasswordRequest;
import com.kinetirx.dto.AuthDtos.CurrentUser;
import com.kinetirx.dto.AuthDtos.LoginRequest;
import com.kinetirx.dto.AuthDtos.LoginResponse;
import com.kinetirx.dto.AuthDtos.SetupRequest;
import com.kinetirx.dto.AuthDtos.SetupStatus;
import com.kinetirx.security.EmployeePrincipal;
import com.kinetirx.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/** /api/auth/* — thin: validation and rules live in AuthService. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final RateLimiter rateLimiter;

    public AuthController(AuthService auth, RateLimiter rateLimiter) {
        this.auth = auth;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody(required = false) LoginRequest req, HttpServletRequest http) {
        rateLimiter.check("login", http, 10, Duration.ofMinutes(1));
        if (req == null) {
            throw com.kinetirx.exception.ApiException.badRequest("identifier and password are required");
        }
        return auth.login(req.identifier(), req.password());
    }

    @GetMapping("/setup-status")
    public SetupStatus setupStatus() {
        return auth.setupStatus();
    }

    @PostMapping("/setup")
    public ResponseEntity<LoginResponse> setup(@RequestBody(required = false) SetupRequest req, HttpServletRequest http) {
        rateLimiter.check("setup", http, 5, Duration.ofMinutes(1));
        if (req == null) {
            throw com.kinetirx.exception.ApiException.badRequest("name and password are required");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.setup(req.name(), req.password()));
    }

    @GetMapping("/me")
    public CurrentUser me(@AuthenticationPrincipal EmployeePrincipal principal) {
        return auth.me(principal.employeeId());
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal EmployeePrincipal principal,
                                               @RequestBody(required = false) ChangePasswordRequest req,
                                               HttpServletRequest http) {
        rateLimiter.check("password", http, 10, Duration.ofMinutes(1));
        if (req == null) {
            throw com.kinetirx.exception.ApiException.badRequest("currentPassword and newPassword are required");
        }
        auth.changePassword(principal.employeeId(), req.currentPassword(), req.newPassword());
        return ResponseEntity.noContent().build();
    }
}
