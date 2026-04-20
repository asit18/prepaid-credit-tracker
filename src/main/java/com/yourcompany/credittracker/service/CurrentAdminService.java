package com.yourcompany.credittracker.service;

import com.yourcompany.credittracker.model.AdminUser;
import com.yourcompany.credittracker.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentAdminService {
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUser currentAdmin(Authentication authentication) {
        String email = email(authentication);
        return adminUserRepository.findByEmailIgnoreCase(email)
                .filter(AdminUser::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated admin not found"));
    }

    public void verifyPassword(AdminUser admin, String password) {
        if (admin.getPasswordHash() == null || !passwordEncoder.matches(password, admin.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid password");
        }
    }

    private String email(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User oauth2User) {
            Object email = oauth2User.getAttributes().get("email");
            return email == null ? authentication.getName() : email.toString();
        }
        return authentication.getName();
    }
}
