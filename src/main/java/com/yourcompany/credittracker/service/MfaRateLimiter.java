package com.yourcompany.credittracker.service;

import com.yourcompany.credittracker.security.SecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MfaRateLimiter {
    private final SecurityProperties securityProperties;
    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();

    public void checkAllowed(String key) {
        AttemptState state = attempts.get(key);
        if (state == null) {
            return;
        }
        if (state.lockedUntil != null && Instant.now().isBefore(state.lockedUntil)) {
            throw new IllegalStateException("Too many invalid MFA attempts. Try again later.");
        }
        if (state.lockedUntil != null && Instant.now().isAfter(state.lockedUntil)) {
            attempts.remove(key);
        }
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    public void recordFailure(String key) {
        attempts.compute(key, (ignored, current) -> {
            AttemptState state = current == null ? new AttemptState(0, null) : current;
            int failures = state.failures + 1;
            if (failures >= securityProperties.mfa().maxInvalidAttempts()) {
                return new AttemptState(failures,
                        Instant.now().plusSeconds(securityProperties.mfa().lockoutMinutes() * 60L));
            }
            return new AttemptState(failures, null);
        });
    }

    private record AttemptState(int failures, Instant lockedUntil) {
    }
}
