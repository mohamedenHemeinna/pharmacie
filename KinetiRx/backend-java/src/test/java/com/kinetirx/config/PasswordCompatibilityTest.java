package com.kinetirx.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

/** bcrypt hashes written by the Go backend (cost 12, $2a$) must verify unchanged in Java. */
class PasswordCompatibilityTest {

    static final String GO_HASH = "$2a$12$Y29p/ntBEx9/30DqCB164unb2vE53o/ZeRStAbjjto6tKOz9XQ7Ia";

    @Test
    void verifiesGoBcryptHash() {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder(12);
        assertTrue(enc.matches("fixture-pass", GO_HASH));
        assertFalse(enc.matches("wrong-pass", GO_HASH));
    }
}
