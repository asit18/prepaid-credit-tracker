package com.yourcompany.credittracker.controller;

import com.yourcompany.credittracker.dto.*;
import com.yourcompany.credittracker.model.AdminUser;
import com.yourcompany.credittracker.service.AuthService;
import com.yourcompany.credittracker.service.CurrentAdminService;
import com.yourcompany.credittracker.service.MfaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/mfa")
@RequiredArgsConstructor
public class MfaController {
    private final MfaService mfaService;
    private final AuthService authService;
    private final CurrentAdminService currentAdminService;

    @PostMapping("/setup")
    public MfaSetupResponse setup(Authentication authentication) {
        AdminUser admin = currentAdminService.currentAdmin(authentication);
        return mfaService.setup(admin.getId());
    }

    @PostMapping("/confirm")
    public MfaConfirmResponse confirm(Authentication authentication, @Valid @RequestBody MfaConfirmRequest request) {
        AdminUser admin = currentAdminService.currentAdmin(authentication);
        return mfaService.confirm(admin.getId(), request.code());
    }

    @PostMapping("/recovery")
    public AuthTokenResponse recovery(@Valid @RequestBody MfaRecoveryRequest request) {
        return new AuthTokenResponse(authService.recover(request.tempToken(), request.backupCode()));
    }

    @PostMapping("/disable")
    public ResponseEntity<Map<String, Boolean>> disable(Authentication authentication,
                                                       @Valid @RequestBody DisableMfaRequest request) {
        AdminUser admin = currentAdminService.currentAdmin(authentication);
        currentAdminService.verifyPassword(admin, request.password());
        mfaService.disable(admin.getId(), request.code());
        return ResponseEntity.ok(Map.of("success", true));
    }
}
