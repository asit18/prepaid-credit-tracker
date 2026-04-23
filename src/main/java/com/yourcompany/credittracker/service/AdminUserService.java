package com.yourcompany.credittracker.service;

import com.yourcompany.credittracker.exception.NotFoundException;
import com.yourcompany.credittracker.model.AdminRole;
import com.yourcompany.credittracker.model.AdminUser;
import com.yourcompany.credittracker.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminUserService {
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.seed-email:admin@example.com}")
    private String seedEmail;

    @Value("${app.local-admin.email:admin@example.com}")
    private String localAdminEmail;

    @Value("${app.local-admin.password:}")
    private String localAdminPassword;

    @Transactional
    public void bootstrapAdmins() {
        if (adminUserRepository.count() == 0) {
            addAdmin(seedEmail, "Seed Admin", null, AdminRole.EMPLOYEE);
        }
        if (localAdminEmail != null && !localAdminEmail.isBlank()) {
            AdminUser localAdmin = addAdmin(localAdminEmail, "Local Admin", localAdminPassword, AdminRole.OWNER);
            localAdmin.setRole(AdminRole.OWNER);
        }
    }

    @Transactional(readOnly = true)
    public boolean isActiveAdmin(String email) {
        return email != null && adminUserRepository.findByEmailIgnoreCase(email)
                .map(AdminUser::isActive)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Optional<AdminUser> findActiveByEmail(String email) {
        return email == null ? Optional.empty() : adminUserRepository.findByEmailIgnoreCase(email)
                .filter(AdminUser::isActive);
    }

    @Transactional(readOnly = true)
    public List<AdminUser> all() {
        return adminUserRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<AdminUser> activeOwners() {
        return adminUserRepository.findByRoleAndActiveTrue(AdminRole.OWNER);
    }

    @Transactional
    public AdminUser addAdmin(String email, String displayName) {
        return addAdmin(email, displayName, null, AdminRole.EMPLOYEE);
    }

    @Transactional
    public AdminUser addAdmin(String email, String displayName, AdminRole role) {
        return addAdmin(email, displayName, null, role);
    }

    @Transactional
    public AdminUser addAdmin(String email, String displayName, String password, AdminRole role) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        return adminUserRepository.findByEmailIgnoreCase(normalized).orElseGet(() -> {
            AdminUser user = new AdminUser();
            user.setEmail(normalized);
            user.setDisplayName(displayName == null || displayName.isBlank() ? normalized : displayName);
            user.setRole(role == null ? AdminRole.EMPLOYEE : role);
            if (password != null && !password.isBlank()) {
                user.setPasswordHash(passwordEncoder.encode(validatePassword(password)));
            }
            user.setActive(true);
            return adminUserRepository.save(user);
        });
    }

    @Transactional
    public AdminUser updateRole(Long id, AdminRole role) {
        AdminUser user = adminUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Admin user not found"));
        user.setRole(role == null ? AdminRole.EMPLOYEE : role);
        return user;
    }

    @Transactional
    public AdminUser setActive(Long id, boolean active) {
        AdminUser user = adminUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Admin user not found"));
        user.setActive(active);
        return user;
    }

    @Transactional
    public AdminUser updatePassword(Long id, String password, String confirmPassword) {
        if (password == null || !password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        AdminUser user = adminUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Admin user not found"));
        user.setPasswordHash(passwordEncoder.encode(validatePassword(password)));
        return user;
    }

    private String validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        return password;
    }
}
