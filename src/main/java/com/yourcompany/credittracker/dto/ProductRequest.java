package com.yourcompany.credittracker.dto;

import jakarta.validation.constraints.NotBlank;

public record ProductRequest(@NotBlank String name, String description, @NotBlank String unitLabel, boolean active) {
}
