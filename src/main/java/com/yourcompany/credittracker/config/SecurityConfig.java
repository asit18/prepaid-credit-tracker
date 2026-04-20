package com.yourcompany.credittracker.config;

import com.yourcompany.credittracker.service.AdminUserService;
import com.yourcompany.credittracker.security.JwtAuthenticationFilter;
import com.yourcompany.credittracker.security.MfaAuthenticationSuccessHandler;
import com.yourcompany.credittracker.security.MfaSessionFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(com.yourcompany.credittracker.security.SecurityProperties.class)
public class SecurityConfig {
    private final AdminUserService adminUserService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final MfaSessionFilter mfaSessionFilter;
    private final MfaAuthenticationSuccessHandler mfaAuthenticationSuccessHandler;

    @Value("${app.auth.google-enabled:false}")
    private boolean googleEnabled;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/login", "/access-denied",
                        "/password-reset", "/api/auth/login", "/api/auth/mfa/verify", "/api/mfa/recovery").permitAll()
                .requestMatchers("/admins/**", "/products/**", "/settings/**", "/api/v1/products/**").hasRole("OWNER")
                .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"))
                .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(mfaSessionFilter, UsernamePasswordAuthenticationFilter.class);

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
                        mfaAuthenticationSuccessHandler.onAuthenticationSuccess(request, response, authentication);
                    }));
        } else {
            http.formLogin(form -> form
                    .loginPage("/login")
                    .successHandler(mfaAuthenticationSuccessHandler)
                    .permitAll())
                    .userDetailsService(username -> adminUserService.findActiveByEmail(username)
                            .filter(admin -> admin.getPasswordHash() != null && !admin.getPasswordHash().isBlank())
                            .map(admin -> new User(admin.getEmail(), admin.getPasswordHash(),
                                    List.of(new SimpleGrantedAuthority("ROLE_" + admin.getRole().name()))))
                            .orElseThrow(() -> new BadCredentialsException("Unknown user")));
        }
        return http.build();
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
