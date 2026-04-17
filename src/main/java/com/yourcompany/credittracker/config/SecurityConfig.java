package com.yourcompany.credittracker.config;

import com.yourcompany.credittracker.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final AdminUserService adminUserService;

    @Value("${app.auth.google-enabled:false}")
    private boolean googleEnabled;

    @Value("${app.local-admin.email:admin@example.com}")
    private String localAdminEmail;

    @Value("${app.local-admin.password:}")
    private String localAdminPassword;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/login", "/access-denied").permitAll()
                .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"))
                .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll());

        if (googleEnabled) {
            http.oauth2Login(oauth -> oauth
                    .loginPage("/login")
                    .successHandler((request, response, authentication) -> {
                        String email = email(authentication);
                        if (!adminUserService.isActiveAdmin(email)) {
                            request.getSession().invalidate();
                            response.sendRedirect("/access-denied");
                            return;
                        }
                        response.sendRedirect("/");
                    }));
        } else {
            http.formLogin(form -> form
                    .loginPage("/login")
                    .successHandler((request, response, authentication) -> response.sendRedirect("/"))
                    .permitAll())
                    .userDetailsService(username -> {
                        if (localAdminEmail.equalsIgnoreCase(username)) {
                            return new User(localAdminEmail, passwordEncoder().encode(localAdminPassword),
                                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
                        }
                        throw new BadCredentialsException("Unknown user");
                    });
        }
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private String email(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof DefaultOAuth2User user) {
            Object email = user.getAttributes().get("email");
            return email == null ? null : email.toString();
        }
        return authentication.getName();
    }
}
