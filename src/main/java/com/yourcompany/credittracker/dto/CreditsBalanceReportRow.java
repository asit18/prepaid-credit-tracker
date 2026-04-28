package com.yourcompany.credittracker.dto;

import java.math.BigDecimal;

public record CreditsBalanceReportRow(String customerName, String productName, BigDecimal credits) {
}
