package com.stockpilot.common.crypto;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Symmetric encryption for secrets stored at rest (currently OAuth tokens for the
 * Google Drive integration). Uses AES-256-GCM with a random IV per value; the IV is
 * prepended to the ciphertext and the whole thing is Base64-encoded for storage in a
 * plain text column.
 *
 * <p>The 256-bit key is derived (SHA-256) from the configured passphrase, so any
 * non-empty string works as a key and rotating it invalidates previously stored values.
 */
@Component
public class TokenCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final String passphrase;
    private final SecureRandom random = new SecureRandom();
    private SecretKeySpec key;

    public TokenCipher(@Value("${app.integrations.google-drive.token-encryption-key}") String passphrase) {
        this.passphrase = passphrase;
    }

    @PostConstruct
    void init() throws Exception {
        if (passphrase == null || passphrase.isBlank()) {
            throw new IllegalStateException("app.integrations.google-drive.token-encryption-key must be set");
        }
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(passphrase.getBytes(StandardCharsets.UTF_8));
        this.key = new SecretKeySpec(digest, "AES");
    }

    /** Encrypts a UTF-8 string, returning a Base64 blob of {@code iv || ciphertext+tag}. */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt token", ex);
        }
    }

    /** Reverses {@link #encrypt(String)}. */
    public String decrypt(String stored) {
        if (stored == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(stored);
            byte[] iv = new byte[IV_BYTES];
            System.arraycopy(combined, 0, iv, 0, IV_BYTES);
            byte[] ciphertext = new byte[combined.length - IV_BYTES];
            System.arraycopy(combined, IV_BYTES, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt token", ex);
        }
    }
}
