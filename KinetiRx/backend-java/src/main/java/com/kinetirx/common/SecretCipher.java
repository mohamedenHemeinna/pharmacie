package com.kinetirx.common;

import com.kinetirx.exception.ApiException;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * AES-256-GCM for secrets at rest (S3 secret key, AI API key). Output layout is
 * nonce(12) || ciphertext || tag(16), identical to the Go crypto package, so existing
 * encrypted rows decrypt unchanged.
 */
@Component
public class SecretCipher {

    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private final SecureRandom random = new SecureRandom();
    private final byte[] key;

    public SecretCipher(com.kinetirx.config.KinetiRxProperties props) {
        this.key = parseKey(props.backupEncryptionKey());
    }

    public boolean isConfigured() {
        return key != null;
    }

    /** Hex key from BACKUP_ENCRYPTION_KEY: exactly 32 bytes (openssl rand -hex 32). */
    static byte[] parseKey(String hex) {
        if (hex == null || hex.isBlank()) return null;
        byte[] k;
        try {
            k = HexFormat.of().parseHex(hex.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("BACKUP_ENCRYPTION_KEY must be hex-encoded", e);
        }
        if (k.length != 32) {
            throw new IllegalStateException("BACKUP_ENCRYPTION_KEY must decode to exactly 32 bytes (got " + k.length + ")");
        }
        return k;
    }

    /** Throws 412 (not_configured) when no key is set, as the Go handlers do. */
    public void requireConfigured() {
        if (key == null) {
            throw new ApiException(412, "not_configured",
                    "This feature isn't available on this deployment yet — set BACKUP_ENCRYPTION_KEY (32 random bytes, e.g. `openssl rand -hex 32`) as a server environment variable and restart the backend, then try again.");
        }
    }

    public byte[] encrypt(String plaintext) {
        requireConfigured();
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            random.nextBytes(nonce);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, nonce));
            byte[] ct = c.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] out = new byte[nonce.length + ct.length];
            System.arraycopy(nonce, 0, out, 0, nonce.length);
            System.arraycopy(ct, 0, out, nonce.length, ct.length);
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("encrypt failed", e);
        }
    }

    public String decrypt(byte[] sealed) {
        requireConfigured();
        if (sealed == null || sealed.length < NONCE_BYTES) {
            throw new IllegalStateException("ciphertext too short");
        }
        try {
            byte[] nonce = java.util.Arrays.copyOfRange(sealed, 0, NONCE_BYTES);
            byte[] ct = java.util.Arrays.copyOfRange(sealed, NONCE_BYTES, sealed.length);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, nonce));
            return new String(c.doFinal(ct), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("decrypt (wrong key, or data tampered)", e);
        }
    }
}
