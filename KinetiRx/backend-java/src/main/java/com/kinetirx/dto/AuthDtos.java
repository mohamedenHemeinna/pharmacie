package com.kinetirx.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/** Request and response shapes for /api/auth/*. Field names match the Go JSON exactly. */
public final class AuthDtos {

    private AuthDtos() {}

    public record LoginRequest(String identifier, String password) {}

    public record ChangePasswordRequest(String currentPassword, String newPassword) {}

    public record SetupRequest(String name, String password) {}

    public record SetupStatus(boolean needsSetup) {}

    public record CurrentUser(String id, String name, String desig, String role,
                              List<String> permissions, boolean mustChangePassword) {}

    public record LoginResponse(String accessToken, String expiresAt, CurrentUser user) {}
}
