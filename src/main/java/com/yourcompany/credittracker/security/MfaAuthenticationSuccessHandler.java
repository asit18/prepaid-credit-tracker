package com.yourcompany.credittracker.security;

import com.yourcompany.credittracker.service.AdminUserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class MfaAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final AdminUserService adminUserService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        adminUserService.findActiveByEmail(email(authentication))
                .ifPresent(admin -> request.getSession().setAttribute(
                        MfaSessionConstants.MFA_VERIFIED, !admin.isMfaEnabled()));
        response.sendRedirect(Boolean.TRUE.equals(request.getSession().getAttribute(MfaSessionConstants.MFA_VERIFIED))
                ? "/"
                : "/login/mfa");
    }

    private String email(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User oauth2User) {
            Object email = oauth2User.getAttributes().get("email");
            return email == null ? authentication.getName() : email.toString();
        }
        return authentication.getName();
    }
}
