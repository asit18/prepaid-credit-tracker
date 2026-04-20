package com.yourcompany.credittracker.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaRecoveryRequest(@NotBlank String tempToken, @NotBlank String backupCode) {
}
