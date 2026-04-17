package com.yourcompany.credittracker.dto;

import com.yourcompany.credittracker.model.TransactionType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransactionRequest(@NotNull Long productId,
                                 @NotNull TransactionType transactionType,
                                 BigDecimal amountPaid,
                                 BigDecimal units,
                                 String notes) {
}
