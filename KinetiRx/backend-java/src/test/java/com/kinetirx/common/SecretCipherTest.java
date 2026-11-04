package com.kinetirx.common;

import com.kinetirx.config.KinetiRxProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class SecretCipherTest {

    /** Key 000102...1f, and a ciphertext sealed by the Go crypto package with that key. */
    static final String KEY_HEX = "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f";
    static final String GO_CIPHER_HEX = "0cd288cd55f3ac333718f6de93e88731bc9b5cc3edab3946da2de85fc7500e77d6d85abad3f3e466f6c298";

    private static SecretCipher cipher(String key) {
        return new SecretCipher(new KinetiRxProperties("0123456789abcdef", Duration.ofHours(1), null, null, null, key));
    }

    @Test
    void decryptsCiphertextSealedByGoBackend() {
        byte[] sealed = HexFormat.of().parseHex(GO_CIPHER_HEX);
        assertEquals("s3-secret-value", cipher(KEY_HEX).decrypt(sealed));
    }

    @Test
    void roundTripsAndUsesNonceLayout() {
        SecretCipher c = cipher(KEY_HEX);
        byte[] sealed = c.encrypt("hello");
        // nonce(12) + plaintext(5) + tag(16)
        assertEquals(12 + 5 + 16, sealed.length);
        assertEquals("hello", c.decrypt(sealed));
        assertFalse(java.util.Arrays.equals(sealed, c.encrypt("hello")), "nonce must be random");
    }

    @Test
    void wrongKeyFailsToDecrypt() {
        byte[] sealed = HexFormat.of().parseHex(GO_CIPHER_HEX);
        SecretCipher other = cipher("ff".repeat(32));
        assertThrows(IllegalStateException.class, () -> other.decrypt(sealed));
    }

    @Test
    void tamperedCiphertextFails() {
        byte[] sealed = HexFormat.of().parseHex(GO_CIPHER_HEX);
        sealed[sealed.length - 1] ^= 0x01;
        assertThrows(IllegalStateException.class, () -> cipher(KEY_HEX).decrypt(sealed));
    }

    @Test
    void rejectsKeyOfWrongLength() {
        assertThrows(IllegalStateException.class, () -> SecretCipher.parseKey("abcd"));
        assertThrows(IllegalStateException.class, () -> SecretCipher.parseKey("zz".repeat(32)));
    }

    @Test
    void unconfiguredKeyRefusesToEncrypt() {
        SecretCipher c = cipher(null);
        assertFalse(c.isConfigured());
        assertThrows(com.kinetirx.exception.ApiException.class, () -> c.encrypt("x"));
    }
}
