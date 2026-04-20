package com.yourcompany.credittracker.service;

import com.yourcompany.credittracker.model.AdminUser;
import com.yourcompany.credittracker.model.PasswordResetToken;
import com.yourcompany.credittracker.repository.AdminUserRepository;
import com.yourcompany.credittracker.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private static final int TOKEN_BYTES = 32;

    private final AdminUserRepository adminUserRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final AdminUserService adminUserService;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.public-base-url:http://localhost:8090}")
    private String publicBaseUrl;

    @Transactional
    public void triggerReset(Long adminId, String actorEmail) {
        AdminUser target = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));
        String token = newToken();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setAdminUser(target);
        resetToken.setTokenHash(hash(token));
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        resetToken.setCreatedBy(actorEmail);
        tokenRepository.save(resetToken);

        String resetUrl = publicBaseUrl + "/password-reset?token=" + token;
        notificationService.send(target.getEmail(), "Password reset requested",
                "A password reset was requested for your admin account.\n\n"
                        + "Use this link within 30 minutes:\n" + resetUrl + "\n\n"
                        + "If you did not expect this, contact the owner.");

        adminUserService.activeOwners().forEach(owner -> notificationService.send(owner.getEmail(),
                "Admin password reset triggered",
                "Password reset was triggered by " + actorEmail + " for " + target.getEmail() + "."));

        auditService.record("admin_password_reset_requested", actorEmail, target.getEmail(),
                "Password reset email sent; expiresAt=" + resetToken.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public boolean isValid(String token) {
        return tokenRepository.findByTokenHash(hash(token))
                .filter(t -> t.getUsedAt() == null)
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    @Transactional
    public void resetPassword(String token, String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(hash(token))
                .filter(t -> t.getUsedAt() == null)
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new IllegalArgumentException("Password reset link is invalid or expired"));
        AdminUser admin = resetToken.getAdminUser();
        admin.setPasswordHash(passwordEncoder.encode(password));
        resetToken.setUsedAt(LocalDateTime.now());
        auditService.record("admin_password_changed", admin.getEmail(), admin.getEmail(), "Password reset token used");
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash password reset token", ex);
        }
    }
}
