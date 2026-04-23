package com.northcare.hospitalcore.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM encryption utility for PHI fields.
 * Uses GCM mode for authenticated encryption — provides both confidentiality and integrity.
 */
@Component
public class EncryptionUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    @Value("${northcare.encryption.key:default-dev-key-change-in-prod}")
    private String rawEncryptionKey;

    /**
     * Encrypts plaintext using AES-256-GCM.
     * Output format: Base64(iv || ciphertext+tag)
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            SecretKey key = deriveKey(rawEncryptionKey);
            byte[] iv = generateIv();

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext for use during decryption
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    /**
     * Decrypts AES-256-GCM ciphertext produced by {@link #encrypt(String)}.
     */
    public String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null) return null;
        try {
            SecretKey key = deriveKey(rawEncryptionKey);
            byte[] decoded = Base64.getDecoder().decode(encryptedBase64);

            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }

    /** Derives a 256-bit AES key from the raw key string using SHA-256. */
    private SecretKey deriveKey(String rawKey) throws Exception {
        byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                .digest(rawKey.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(Arrays.copyOf(keyBytes, 32), "AES");
    }

    private byte[] generateIv() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}
