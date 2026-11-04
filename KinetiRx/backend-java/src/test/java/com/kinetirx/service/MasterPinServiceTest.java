package com.kinetirx.service;

import com.kinetirx.entity.SecuritySettings;
import com.kinetirx.exception.ApiException;
import com.kinetirx.repository.SecuritySettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MasterPinServiceTest {

    private final BCryptPasswordEncoder enc = new BCryptPasswordEncoder(4);  // low cost keeps the test fast
    private SecuritySettingsRepository repo;
    private SecuritySettings row;
    private MasterPinService service;

    @BeforeEach
    void setUp() {
        repo = mock(SecuritySettingsRepository.class);
        row = new SecuritySettings();
        when(repo.findById(Boolean.TRUE)).thenReturn(Optional.of(row));
        when(repo.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        service = new MasterPinService(repo, enc);
    }

    @Test
    void rejectsPinOutsideFourToEightDigits() {
        assertThrows(ApiException.class, () -> service.set(null, "12"));
        assertThrows(ApiException.class, () -> service.set(null, "123456789"));
        assertThrows(ApiException.class, () -> service.set(null, "12a4"));
    }

    @Test
    void rotatingRequiresCurrentPin() {
        row.setMasterPinHash(enc.encode("4321"));
        ApiException ex = assertThrows(ApiException.class, () -> service.set("0000", "9999"));
        assertEquals(401, ex.status());
    }

    @Test
    void fiveWrongAttemptsLockTheAccount() {
        row.setMasterPinHash(enc.encode("4321"));
        for (int i = 0; i < 4; i++) {
            assertFalse(service.verify("0000").valid());
        }
        assertFalse(service.verify("0000").valid());   // fifth failure locks
        assertNotNull(row.getLockedUntil());
        ApiException locked = assertThrows(ApiException.class, () -> service.verify("4321"));
        assertEquals(423, locked.status());
        assertEquals("locked", locked.type());
    }

    @Test
    void lockExpires() {
        row.setMasterPinHash(enc.encode("4321"));
        row.setLockedUntil(Instant.now().minus(1, ChronoUnit.MINUTES));
        assertTrue(service.verify("4321").valid());
    }

    @Test
    void correctPinResetsFailureCount() {
        row.setMasterPinHash(enc.encode("4321"));
        service.verify("0000");
        service.verify("0000");
        assertTrue(service.verify("4321").valid());
        assertEquals(0, row.getFailedAttempts());
    }

    @Test
    void verifyWithoutConfiguredPinIsBadRequest() {
        ApiException ex = assertThrows(ApiException.class, () -> service.verify("1234"));
        assertEquals(400, ex.status());
    }

    @Test
    void statusNeverExposesTheHash() {
        row.setMasterPinHash(enc.encode("4321"));
        var status = service.status();
        assertTrue(status.isSet());
        assertNull(status.lockedUntil());
    }
}
