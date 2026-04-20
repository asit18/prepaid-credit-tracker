package com.yourcompany.credittracker.service;

import com.yourcompany.credittracker.security.SecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AesGcmEncryptionService implements EncryptionService {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecurityProperties securityProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt MFA secret", ex);
        }
    }

    @Override
    public String decrypt(String ciphertext) {
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_BYTES);
            byte[] encrypted = Arrays.copyOfRange(combined, IV_BYTES, combined.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec(), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt MFA secret", ex);
        }
    }

    private SecretKeySpec keySpec() {
        String configured = securityProperties.encryption().aesKeyBase64();
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("APP_AES_KEY_BASE64 is required for MFA secret encryption");
        }
        byte[] key = Base64.getDecoder().decode(configured);
        if (key.length != 32) {
            throw new IllegalStateException("APP_AES_KEY_BASE64 must decode to exactly 32 bytes");
        }
        return new SecretKeySpec(key, "AES");
    }
}
