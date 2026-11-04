package com.kinetirx.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinetirx.config.KinetiRxProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    /** Token signed by the Go backend's own golang-jwt/v5 code (same claim layout as internal/auth/jwt.go). */
    static final String GO_TOKEN_SECRET = "fixture-secret-0123456789abcdef";
    static final String GO_TOKEN =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlaWQiOiJFTVAtQURNSU4tMSIsIm5hbWUiOiJNYXN0ZXIgQWRtaW4iLCJyb2xlIjoiYWRtaW4iLCJwZXJtaXNzaW9ucyI6WyJpbnZlbnRvcnkiLCJwYXRpZW50cyJdLCJpc3MiOiJraW5ldGlyeC1iYWNrZW5kIiwic3ViIjoiRU1QLUFETUlOLTEiLCJleHAiOjQxMDI0NDQ4MDAsIm5iZiI6MTc2NzIyNTYwMCwiaWF0IjoxNzY3MjI1NjAwfQ.xjoa0mZej-sg7GrKtNBoHr02a3cQQxQswD540QkU610";

    private static JwtService service(String secret, Duration ttl) {
        KinetiRxProperties props = new KinetiRxProperties(secret, ttl, null, null, null, null);
        return new JwtService(props, new ObjectMapper());
    }

    @Test
    void verifiesTokenIssuedByGoBackend() {
        JwtService jwt = service(GO_TOKEN_SECRET, Duration.ofHours(12));
        var claims = jwt.verify(GO_TOKEN).orElseThrow();
        assertEquals("EMP-ADMIN-1", claims.employeeId());
        assertEquals("Master Admin", claims.name());
        assertEquals("admin", claims.role());
        assertEquals(List.of("inventory", "patients"), claims.permissions());
    }

    @Test
    void issuedTokenCarriesGoClaimLayout() throws Exception {
        JwtService jwt = service("0123456789abcdef", Duration.ofHours(12));
        String token = jwt.issue("EMP-1", "Ann", "staff", List.of("pos")).token();
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
        String header = new String(Base64.getUrlDecoder().decode(parts[0]));
        assertEquals("{\"alg\":\"HS256\",\"typ\":\"JWT\"}", header);
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        for (String key : List.of("\"eid\":\"EMP-1\"", "\"name\":\"Ann\"", "\"role\":\"staff\"", "\"iss\":\"kinetirx-backend\"", "\"sub\":\"EMP-1\"")) {
            assertTrue(payload.contains(key), "missing " + key + " in " + payload);
        }
        assertTrue(jwt.verify(token).isPresent());
    }

    @Test
    void rejectsTamperedPayload() throws Exception {
        JwtService jwt = service(GO_TOKEN_SECRET, Duration.ofHours(12));
        String[] parts = GO_TOKEN.split("\\.");
        String forged = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"eid\":\"EMP-ADMIN-1\",\"role\":\"admin\",\"exp\":4102444800,\"nbf\":0}".getBytes());
        assertTrue(jwt.verify(parts[0] + "." + forged + "." + parts[2]).isEmpty());
    }

    @Test
    void rejectsWrongSecret() {
        assertTrue(service("another-secret-0123456789", Duration.ofHours(12)).verify(GO_TOKEN).isEmpty());
    }

    @Test
    void rejectsNoneAndOtherAlgorithms() {
        String noneHeader = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes());
        String[] parts = GO_TOKEN.split("\\.");
        assertTrue(service(GO_TOKEN_SECRET, Duration.ofHours(12)).verify(noneHeader + "." + parts[1] + "." + parts[2]).isEmpty());
    }

    @Test
    void rejectsExpiredToken() {
        // A negative TTL issues a token that is already expired.
        JwtService jwt = service("0123456789abcdef", Duration.ofSeconds(-60));
        String token = jwt.issue("EMP-1", "Ann", "staff", List.of()).token();
        assertTrue(jwt.verify(token).isEmpty());
    }

    @Test
    void acceptsShortSecretsLikeGoDoes() {
        // Go accepts any JWT_SECRET of 16+ characters; jjwt would reject this HS256 key.
        JwtService jwt = service("sixteen-chars-ok", Duration.ofHours(1));
        assertTrue(jwt.verify(jwt.issue("E", "n", "staff", List.of()).token()).isPresent());
    }

    @Test
    void rejectsMalformedTokens() {
        JwtService jwt = service(GO_TOKEN_SECRET, Duration.ofHours(12));
        assertTrue(jwt.verify(null).isEmpty());
        assertTrue(jwt.verify("").isEmpty());
        assertTrue(jwt.verify("a.b").isEmpty());
        assertTrue(jwt.verify("not.a.jwt").isEmpty());
    }
}
