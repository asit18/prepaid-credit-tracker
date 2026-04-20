package com.yourcompany.credittracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "admin_users")
public class AdminUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminRole role = AdminRole.EMPLOYEE;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled;

    @Column(name = "mfa_secret", columnDefinition = "TEXT")
    private String mfaSecret;

    @Column(name = "mfa_setup_pending", nullable = false)
    private boolean mfaSetupPending;

    @Column(name = "backup_codes", columnDefinition = "TEXT")
    private String backupCodes;

    @Column(name = "last_used_totp_step")
    private Long lastUsedTotpStep;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
