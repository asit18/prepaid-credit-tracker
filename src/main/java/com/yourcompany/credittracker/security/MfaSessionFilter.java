package com.yourcompany.credittracker.security;

import com.yourcompany.credittracker.service.AdminUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class MfaSessionFilter extends OncePerRequestFilter {
    private final AdminUserService adminUserService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isAllowed(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal() instanceof String) {
            filterChain.doFilter(request, response);
            return;
        }

        adminUserService.findActiveByEmail(email(authentication))
                .filter(admin -> admin.isMfaEnabled())
                .ifPresent(admin -> {
                    Object verified = request.getSession().getAttribute(MfaSessionConstants.MFA_VERIFIED);
                    if (!Boolean.TRUE.equals(verified)) {
                        try {
                            response.sendRedirect("/login/mfa");
                        } catch (IOException ex) {
                            throw new IllegalStateException("Failed to redirect to MFA challenge", ex);
                        }
                    }
                });

        if (response.isCommitted()) {
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected void doFilterNestedErrorDispatch(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        filterChain.doFilter(request, response);
    }

    private boolean isAllowed(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/api/")
                || path.equals("/login")
                || path.equals("/login/mfa")
                || path.equals("/logout")
                || path.equals("/access-denied");
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
