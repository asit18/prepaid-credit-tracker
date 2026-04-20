package com.yourcompany.credittracker.service;

import com.yourcompany.credittracker.dto.LoginResponse;
import com.yourcompany.credittracker.model.AdminUser;
import com.yourcompany.credittracker.repository.AdminUserRepository;
import com.yourcompany.credittracker.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MfaService mfaService;
    private final MfaRateLimiter rateLimiter;

    @Transactional(readOnly = true)
    public LoginResponse login(String email, String password) {
        AdminUser user = adminUserRepository.findByEmailIgnoreCase(email)
                .filter(AdminUser::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        if (user.isMfaEnabled()) {
            return LoginResponse.mfaRequired(jwtService.createMfaTempToken(user));
        }
        return LoginResponse.authenticated(jwtService.createAccessToken(user));
    }

    @Transactional
    public String verifyMfa(String tempToken, String code) {
        Long userId = jwtService.validateMfaTempTokenAndGetUserId(tempToken);
        String limiterKey = "mfa:" + userId;
        rateLimiter.checkAllowed(limiterKey);
        AdminUser user = mfaService.lockedUser(userId);
        try {
            mfaService.verifyTotpForUser(user, code);
            rateLimiter.recordSuccess(limiterKey);
            return jwtService.createAccessToken(user);
        } catch (RuntimeException ex) {
            rateLimiter.recordFailure(limiterKey);
            throw ex;
        }
    }

    @Transactional
    public String recover(String tempToken, String backupCode) {
        Long userId = jwtService.validateMfaTempTokenAndGetUserId(tempToken);
        String limiterKey = "mfa:" + userId;
        rateLimiter.checkAllowed(limiterKey);
        AdminUser user = mfaService.lockedUser(userId);
        try {
            mfaService.consumeRecoveryCode(user, backupCode);
            rateLimiter.recordSuccess(limiterKey);
            return jwtService.createAccessToken(user);
        } catch (RuntimeException ex) {
            rateLimiter.recordFailure(limiterKey);
            throw ex;
        }
    }
}
