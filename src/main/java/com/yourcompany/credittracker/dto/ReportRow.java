package com.yourcompany.credittracker.dto;

import com.yourcompany.credittracker.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReportRow(String customer, String product, LocalDateTime date, TransactionType type, BigDecimal units,
                        BigDecimal amountPaid, BigDecimal pricePerUnit, BigDecimal balanceAfter, String createdBy) {
}
