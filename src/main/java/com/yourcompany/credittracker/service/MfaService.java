package com.yourcompany.credittracker.service;

import com.yourcompany.credittracker.dto.MfaConfirmResponse;
import com.yourcompany.credittracker.dto.MfaSetupResponse;
import com.yourcompany.credittracker.model.AdminUser;
import com.yourcompany.credittracker.repository.AdminUserRepository;
import com.yourcompany.credittracker.security.SecurityProperties;
import com.yourcompany.credittracker.util.Base32SecretGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class MfaService {
    private final AdminUserRepository adminUserRepository;
    private final Base32SecretGenerator secretGenerator;
    private final EncryptionService encryptionService;
    private final TotpService totpService;
    private final QrCodeService qrCodeService;
    private final BackupCodeService backupCodeService;
    private final SecurityProperties securityProperties;
    private final AuditService auditService;
    private final MfaRateLimiter rateLimiter;

    @Transactional
    public MfaSetupResponse setup(Long userId) {
        AdminUser user = lockedUser(userId);
        String secret = secretGenerator.generate();
        user.setMfaSecret(encryptionService.encrypt(secret));
        user.setMfaEnabled(false);
        user.setMfaSetupPending(true);
        user.setBackupCodes(null);
        user.setLastUsedTotpStep(null);

        String otpauthUri = buildOtpAuthUri(user.getEmail(), secret);
        auditService.mfaSetupStarted(user.getId(), user.getEmail());
        return new MfaSetupResponse(secret, otpauthUri, qrCodeService.generateBase64Png(otpauthUri));
    }

    @Transactional
    public MfaConfirmResponse confirm(Long userId, String code) {
        AdminUser user = lockedUser(userId);
        String limiterKey = "mfa-confirm:" + user.getId();
        rateLimiter.checkAllowed(limiterKey);
        if (!user.isMfaSetupPending() || user.getMfaSecret() == null) {
            throw new IllegalStateException("MFA setup is not pending");
        }
        TotpService.TotpVerificationResult result = verifySecret(user, code);
        if (!result.valid()) {
            auditService.mfaVerificationFailure(user.getId(), user.getEmail());
            rateLimiter.recordFailure(limiterKey);
            throw new IllegalArgumentException("Invalid MFA code");
        }
        rateLimiter.recordSuccess(limiterKey);
        user.setMfaEnabled(true);
        user.setMfaSetupPending(false);
        user.setLastUsedTotpStep(result.timeStep());
        BackupCodeService.GeneratedBackupCodes backupCodes = backupCodeService.generate();
        user.setBackupCodes(backupCodes.hashedCodesJson());
        auditService.mfaSetupConfirmed(user.getId(), user.getEmail());
        return new MfaConfirmResponse(true, backupCodes.plainCodes());
    }

    @Transactional
    public void verifyTotpForUser(AdminUser user, String code) {
        if (!user.isMfaEnabled() || user.getMfaSecret() == null) {
            throw new IllegalStateException("MFA is not enabled");
        }
        TotpService.TotpVerificationResult result = verifySecret(user, code);
        if (!result.valid()) {
            auditService.mfaVerificationFailure(user.getId(), user.getEmail());
            throw new IllegalArgumentException("Invalid MFA code");
        }
        if (user.getLastUsedTotpStep() != null && user.getLastUsedTotpStep().equals(result.timeStep())) {
            auditService.mfaVerificationFailure(user.getId(), user.getEmail());
            throw new IllegalArgumentException("MFA code has already been used");
        }
        user.setLastUsedTotpStep(result.timeStep());
        auditService.mfaVerificationSuccess(user.getId(), user.getEmail());
    }

    @Transactional
    public void verifyByUserId(Long userId, String code) {
        verifyTotpForUser(lockedUser(userId), code);
    }

    @Transactional
    public void consumeRecoveryCode(AdminUser user, String backupCode) {
        boolean consumed = backupCodeService.consume(user.getBackupCodes(), backupCode, user::setBackupCodes);
        if (!consumed) {
            auditService.mfaVerificationFailure(user.getId(), user.getEmail());
            throw new IllegalArgumentException("Invalid recovery code");
        }
        auditService.recoveryCodeUsed(user.getId(), user.getEmail());
    }

    @Transactional
    public void disable(Long userId, String code) {
        AdminUser user = lockedUser(userId);
        String limiterKey = "mfa-disable:" + user.getId();
        rateLimiter.checkAllowed(limiterKey);
        try {
            verifyTotpForUser(user, code);
            rateLimiter.recordSuccess(limiterKey);
        } catch (RuntimeException ex) {
            rateLimiter.recordFailure(limiterKey);
            throw ex;
        }
        user.setMfaEnabled(false);
        user.setMfaSetupPending(false);
        user.setMfaSecret(null);
        user.setBackupCodes(null);
        user.setLastUsedTotpStep(null);
        auditService.mfaDisabled(user.getId(), user.getEmail());
    }

    public AdminUser lockedUser(Long userId) {
        return adminUserRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));
    }

    private TotpService.TotpVerificationResult verifySecret(AdminUser user, String code) {
        return totpService.verify(encryptionService.decrypt(user.getMfaSecret()), code);
    }

    private String buildOtpAuthUri(String email, String secret) {
        String issuer = securityProperties.mfa().issuer();
        String label = issuer + ":" + email;
        return "otpauth://totp/"
                + UriUtils.encodePath(label, StandardCharsets.UTF_8)
                + "?secret=" + UriUtils.encodeQueryParam(secret, StandardCharsets.UTF_8)
                + "&issuer=" + UriUtils.encodeQueryParam(issuer, StandardCharsets.UTF_8)
                + "&algorithm=SHA1&digits=6&period=30";
    }
}
