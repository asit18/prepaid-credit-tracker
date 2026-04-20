package com.yourcompany.credittracker.controller;

import com.yourcompany.credittracker.dto.AuthTokenResponse;
import com.yourcompany.credittracker.dto.LoginRequest;
import com.yourcompany.credittracker.dto.LoginResponse;
import com.yourcompany.credittracker.dto.MfaVerifyRequest;
import com.yourcompany.credittracker.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    @PostMapping("/mfa/verify")
    public AuthTokenResponse verifyMfa(@Valid @RequestBody MfaVerifyRequest request) {
        return new AuthTokenResponse(authService.verifyMfa(request.tempToken(), request.code()));
    }
}
