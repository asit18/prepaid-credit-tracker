package com.yourcompany.credittracker.security;

import com.yourcompany.credittracker.repository.AdminUserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final AdminUserRepository adminUserRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.validateAccessToken(authorization.substring(7));
            String email = claims.get("email", String.class);
            adminUserRepository.findByEmailIgnoreCase(email)
                    .filter(user -> user.isActive() && user.getId().toString().equals(claims.getSubject()))
                    .ifPresent(user -> SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(user.getEmail(), null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))));
        } catch (RuntimeException ignored) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
