package com.yourcompany.credittracker.controller;

import com.yourcompany.credittracker.service.AppSettingService;
import com.yourcompany.credittracker.service.CurrentAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {
    private final AppSettingService appSettingService;
    private final CurrentAdminService currentAdminService;

    @ModelAttribute("businessName")
    String businessName() {
        return appSettingService.businessName();
    }

    @ModelAttribute("currentAdminName")
    String currentAdminName(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        try {
            var admin = currentAdminService.currentAdmin(authentication);
            return admin.getDisplayName() == null || admin.getDisplayName().isBlank()
                    ? admin.getEmail()
                    : admin.getDisplayName();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
