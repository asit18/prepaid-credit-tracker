package com.yourcompany.credittracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DisableMfaRequest(
        @NotBlank String password,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "must be a 6 digit code") String code) {
}
