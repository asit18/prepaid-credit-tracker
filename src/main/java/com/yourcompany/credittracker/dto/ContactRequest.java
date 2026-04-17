package com.yourcompany.credittracker.dto;

import jakarta.validation.constraints.NotBlank;

public record ContactRequest(@NotBlank String name, String email, String phone, String relationship) {
}
