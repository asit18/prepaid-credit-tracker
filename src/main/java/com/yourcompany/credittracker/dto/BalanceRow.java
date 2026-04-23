package com.yourcompany.credittracker.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BalanceRow(Long productId, String productName, String unitLabel, BigDecimal currentPrice,
                         BigDecimal balance, String colorHexCode, LocalDateTime lastTransactionDate) {
}
