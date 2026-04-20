package com.yourcompany.credittracker.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaVerifyRequest(@NotBlank String tempToken, @NotBlank String code) {
}
