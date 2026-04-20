package com.yourcompany.credittracker.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(Jwt jwt, Encryption encryption, Mfa mfa) {
    public record Jwt(String issuer, String secret, long accessTokenTtlMinutes, long mfaTempTokenTtlMinutes) {
    }

    public record Encryption(String aesKeyBase64) {
    }

    public record Mfa(String issuer, int digits, int timeStepSeconds, int allowedWindow,
                      int backupCodeCount, int backupCodeLength, int maxInvalidAttempts, int lockoutMinutes) {
    }
}
