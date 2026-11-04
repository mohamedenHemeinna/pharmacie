package com.kinetirx.service;

import com.kinetirx.dto.StaffDtos.MasterPinStatus;
import com.kinetirx.dto.StaffDtos.VerifyMasterPinResponse;
import com.kinetirx.entity.SecuritySettings;
import com.kinetirx.exception.ApiException;
import com.kinetirx.common.TimeFormat;
import com.kinetirx.repository.SecuritySettingsRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;

/**
 * Master Security PIN: a second factor for the highest-risk admin actions.
 * Lockout rules match handlers/security.go: 5 failures lock the PIN for 15 minutes.
 */
@Service
public class MasterPinService {

    private static final Pattern PIN = Pattern.compile("^[0-9]{4,8}$");
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_WINDOW = Duration.ofMinutes(15);

    private final SecuritySettingsRepository settings;
    private final PasswordEncoder encoder;

    public MasterPinService(SecuritySettingsRepository settings, PasswordEncoder encoder) {
        this.settings = settings;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public MasterPinStatus status() {
        SecuritySettings s = load();
        boolean locked = s.getLockedUntil() != null && s.getLockedUntil().isAfter(Instant.now());
        return new MasterPinStatus(s.getMasterPinHash() != null,
                locked ? TimeFormat.rfc3339Seconds(s.getLockedUntil()) : null);
    }

    /** Admin only. Rotating an existing PIN requires the current one. */
    @Transactional
    public MasterPinStatus set(String currentPin, String newPin) {
        if (newPin == null || newPin.isEmpty()) throw ApiException.badRequest("newPin is required");
        if (!PIN.matcher(newPin).matches()) throw ApiException.badRequest("PIN must be 4 to 8 digits");
        SecuritySettings s = load();
        if (s.getMasterPinHash() != null) {
            if (currentPin == null || currentPin.isEmpty() || !encoder.matches(currentPin, s.getMasterPinHash())) {
                throw new ApiException(401, "unauthorized", "Current Master PIN is incorrect");
            }
        }
        s.setMasterPinHash(encoder.encode(newPin));
        s.setFailedAttempts(0);
        s.setLockedUntil(null);
        settings.saveAndFlush(s);
        return new MasterPinStatus(true, null);
    }

    @Transactional
    public VerifyMasterPinResponse verify(String pin) {
        if (pin == null || pin.isEmpty()) throw ApiException.badRequest("pin is required");
        SecuritySettings s = load();
        if (s.getMasterPinHash() == null) {
            throw ApiException.badRequest("No Master PIN has been configured yet — set one in Invoice Settings first");
        }
        if (s.getLockedUntil() != null && s.getLockedUntil().isAfter(Instant.now())) {
            throw new ApiException(423, "locked", "Too many incorrect Master PIN attempts. Try again later.");
        }
        if (encoder.matches(pin, s.getMasterPinHash())) {
            s.setFailedAttempts(0);
            s.setLockedUntil(null);
            settings.saveAndFlush(s);
            return new VerifyMasterPinResponse(true);
        }
        int attempts = s.getFailedAttempts() + 1;
        if (attempts >= MAX_ATTEMPTS) {
            s.setLockedUntil(Instant.now().plus(LOCK_WINDOW));
            attempts = 0;
        }
        s.setFailedAttempts(attempts);
        settings.saveAndFlush(s);
        return new VerifyMasterPinResponse(false);
    }

    private SecuritySettings load() {
        return settings.findById(Boolean.TRUE)
                .orElseThrow(() -> ApiException.internal("Failed to load security settings"));
    }
}
