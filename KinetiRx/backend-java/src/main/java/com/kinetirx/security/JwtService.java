package com.kinetirx.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinetirx.config.KinetiRxProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Issues and verifies HS256 access tokens with exactly the claims the Go backend used:
 * eid, name, role, permissions, sub, iat, nbf, exp, iss="kinetirx-backend".
 *
 * Tokens issued by either backend verify in the other while both share JWT_SECRET,
 * which lets clients stay logged in across the cutover.
 *
 * The HS256 check is written here rather than with jjwt, because jjwt rejects keys under
 * 256 bits and the Go backend accepts any secret of 16+ characters.
 */
@Service
public class JwtService {

    static final String ISSUER = "kinetirx-backend";
    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private final byte[] secret;
    private final Duration ttl;
    private final ObjectMapper mapper;

    public JwtService(KinetiRxProperties props, ObjectMapper mapper) {
        this.secret = props.jwtSecret().getBytes(StandardCharsets.UTF_8);
        this.ttl = props.jwtAccessTokenTtl();
        this.mapper = mapper;
    }

    public record IssuedToken(String token, Instant expiresAt) {}

    public IssuedToken issue(String employeeId, String name, String role, List<String> permissions) {
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        Instant exp = now.plus(ttl);
        Map<String, Object> claims = new java.util.LinkedHashMap<>();
        claims.put("eid", employeeId);
        claims.put("name", name);
        claims.put("role", role);
        claims.put("permissions", permissions);
        claims.put("sub", employeeId);
        claims.put("iat", now.getEpochSecond());
        claims.put("nbf", now.getEpochSecond());
        claims.put("exp", exp.getEpochSecond());
        claims.put("iss", ISSUER);
        try {
            String header = b64(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
            String payload = b64(mapper.writeValueAsBytes(claims));
            String signingInput = header + "." + payload;
            return new IssuedToken(signingInput + "." + b64(hmac(signingInput)), exp);
        } catch (Exception e) {
            throw new IllegalStateException("sign token", e);
        }
    }

    /** Returns the verified claims, or empty for any signature, algorithm, expiry, or shape failure. */
    public Optional<Claims> verify(String token) {
        if (token == null) return Optional.empty();
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) return Optional.empty();
        try {
            Map<String, Object> header = mapper.readValue(
                    Base64.getUrlDecoder().decode(parts[0]), new TypeReference<>() {});
            if (!"HS256".equals(header.get("alg"))) return Optional.empty();

            byte[] expected = hmac(parts[0] + "." + parts[1]);
            byte[] actual = Base64.getUrlDecoder().decode(parts[2]);
            if (!MessageDigest.isEqual(expected, actual)) return Optional.empty();

            Map<String, Object> c = mapper.readValue(
                    Base64.getUrlDecoder().decode(parts[1]), new TypeReference<>() {});
            long now = Instant.now().getEpochSecond();
            long exp = asLong(c.get("exp"), -1);
            long nbf = asLong(c.get("nbf"), 0);
            if (exp < 0 || now >= exp || now < nbf) return Optional.empty();

            @SuppressWarnings("unchecked")
            List<String> perms = c.get("permissions") instanceof List<?> l
                    ? l.stream().map(String::valueOf).toList() : List.of();
            return Optional.of(new Claims(
                    String.valueOf(c.get("eid")),
                    String.valueOf(c.get("name")),
                    String.valueOf(c.get("role")),
                    perms));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public record Claims(String employeeId, String name, String role, List<String> permissions) {}

    private byte[] hmac(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private static String b64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static long asLong(Object v, long fallback) {
        return v instanceof Number n ? n.longValue() : fallback;
    }
}
