package com.yourcompany.credittracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceRequest(@NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal pricePerUnit,
                           @NotNull LocalDate effectiveFrom) {
}
