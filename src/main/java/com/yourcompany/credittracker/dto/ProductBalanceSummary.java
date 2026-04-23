package com.yourcompany.credittracker.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductBalanceSummary(Long productId, BigDecimal balance, LocalDateTime lastTransactionDate) {
}
