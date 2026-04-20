package com.yourcompany.credittracker.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourcompany.credittracker.security.SecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BackupCodeService {
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties securityProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public GeneratedBackupCodes generate() {
        List<String> plainCodes = new ArrayList<>();
        List<String> hashedCodes = new ArrayList<>();
        for (int i = 0; i < securityProperties.mfa().backupCodeCount(); i++) {
            String code = randomCode(securityProperties.mfa().backupCodeLength());
            plainCodes.add(code);
            hashedCodes.add(passwordEncoder.encode(code));
        }
        return new GeneratedBackupCodes(plainCodes, toJson(hashedCodes));
    }

    public boolean consume(String backupCodesJson, String submittedCode, BackupCodeUpdate update) {
        List<String> hashes = fromJson(backupCodesJson);
        for (int i = 0; i < hashes.size(); i++) {
            if (passwordEncoder.matches(normalize(submittedCode), hashes.get(i))) {
                hashes.remove(i);
                update.updatedJson(toJson(hashes));
                return true;
            }
        }
        return false;
    }

    private String randomCode(int length) {
        StringBuilder builder = new StringBuilder(length + 1);
        for (int i = 0; i < length; i++) {
            if (i == 5) {
                builder.append("-");
            }
            builder.append(ALPHABET[secureRandom.nextInt(ALPHABET.length)]);
        }
        return builder.toString();
    }

    private String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }

    private String toJson(List<String> hashes) {
        try {
            return objectMapper.writeValueAsString(hashes);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize backup codes", ex);
        }
    }

    private List<String> fromJson(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read backup codes", ex);
        }
    }

    public record GeneratedBackupCodes(List<String> plainCodes, String hashedCodesJson) {
    }

    @FunctionalInterface
    public interface BackupCodeUpdate {
        void updatedJson(String json);
    }
}
