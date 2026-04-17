package com.yourcompany.credittracker.service;

import com.yourcompany.credittracker.exception.NotFoundException;
import com.yourcompany.credittracker.model.AdminUser;
import com.yourcompany.credittracker.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {
    private final AdminUserRepository adminUserRepository;

    @Value("${app.admin.seed-email:admin@example.com}")
    private String seedEmail;

    @Value("${app.local-admin.email:admin@example.com}")
    private String localAdminEmail;

    @Transactional
    public void bootstrapAdmins() {
        if (adminUserRepository.count() == 0) {
            addAdmin(seedEmail, "Seed Admin");
        }
        if (!adminUserRepository.existsByEmailIgnoreCase(localAdminEmail)) {
            addAdmin(localAdminEmail, "Local Admin");
        }
    }

    @Transactional(readOnly = true)
    public boolean isActiveAdmin(String email) {
        return email != null && adminUserRepository.findByEmailIgnoreCase(email)
                .map(AdminUser::isActive)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<AdminUser> all() {
        return adminUserRepository.findAll();
    }

    @Transactional
    public AdminUser addAdmin(String email, String displayName) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        return adminUserRepository.findByEmailIgnoreCase(normalized).orElseGet(() -> {
            AdminUser user = new AdminUser();
            user.setEmail(normalized);
            user.setDisplayName(displayName == null || displayName.isBlank() ? normalized : displayName);
            user.setActive(true);
            return adminUserRepository.save(user);
        });
    }

    @Transactional
    public AdminUser setActive(Long id, boolean active) {
        AdminUser user = adminUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Admin user not found"));
        user.setActive(active);
        return user;
    }
}
