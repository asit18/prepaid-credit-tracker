package com.yourcompany.credittracker.service;

import com.yourcompany.credittracker.dto.ReportRow;
import com.yourcompany.credittracker.model.CreditTransaction;
import com.yourcompany.credittracker.model.TransactionType;
import com.yourcompany.credittracker.repository.CreditTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final CreditTransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<CreditTransaction> search(LocalDate from, LocalDate to, Long customerId, Long productId, TransactionType type) {
        LocalDateTime fromDate = from == null ? null : from.atStartOfDay();
        LocalDateTime toDate = to == null ? null : to.plusDays(1).atStartOfDay().minusNanos(1);
        return transactionRepository.findAllByOrderByTransactionDateDesc().stream()
                .filter(t -> fromDate == null || !t.getTransactionDate().isBefore(fromDate))
                .filter(t -> toDate == null || !t.getTransactionDate().isAfter(toDate))
                .filter(t -> customerId == null || t.getCustomer().getId().equals(customerId))
                .filter(t -> productId == null || t.getProduct().getId().equals(productId))
                .filter(t -> type == null || t.getTransactionType() == type)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReportRow> rows(LocalDate from, LocalDate to, Long customerId, Long productId, TransactionType type) {
        Map<String, BigDecimal> running = new LinkedHashMap<>();
        List<CreditTransaction> chronological = new ArrayList<>(search(from, to, customerId, productId, type));
        chronological.sort((a, b) -> a.getTransactionDate().compareTo(b.getTransactionDate()));
        List<ReportRow> rows = new ArrayList<>();
        for (CreditTransaction tx : chronological) {
            String key = tx.getCustomer().getId() + ":" + tx.getProduct().getId();
            BigDecimal balance = running.getOrDefault(key, BigDecimal.ZERO).add(tx.getUnits());
            running.put(key, balance);
            rows.add(new ReportRow(tx.getCustomer().getName(), tx.getProduct().getName(), tx.getTransactionDate(),
                    tx.getTransactionType(), tx.getUnits(), tx.getAmountPaid(), tx.getPricePerUnitAtTime(),
                    balance, tx.getCreatedBy()));
        }
        rows.sort((a, b) -> b.date().compareTo(a.date()));
        return rows;
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> summary(LocalDate from, LocalDate to, Long customerId, Long productId, TransactionType type) {
        List<CreditTransaction> txs = search(from, to, customerId, productId, type);
        BigDecimal purchased = txs.stream()
                .filter(t -> t.getTransactionType() == TransactionType.PURCHASE)
                .map(CreditTransaction::getUnits)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal consumed = txs.stream()
                .filter(t -> t.getUnits().signum() < 0)
                .map(t -> t.getUnits().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal net = txs.stream().map(CreditTransaction::getUnits).reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.of("purchased", purchased, "consumed", consumed, "net", net);
    }
}
