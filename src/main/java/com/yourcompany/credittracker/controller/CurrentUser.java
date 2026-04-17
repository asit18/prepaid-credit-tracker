package com.yourcompany.credittracker.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

final class CurrentUser {
    private CurrentUser() {
    }

    static String email(Authentication authentication) {
        if (authentication == null) {
            return "system";
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User oauth2User) {
            Object email = oauth2User.getAttributes().get("email");
            return email == null ? authentication.getName() : email.toString();
        }
        return authentication.getName();
    }
}
