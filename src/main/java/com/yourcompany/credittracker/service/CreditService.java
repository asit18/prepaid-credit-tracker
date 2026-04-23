package com.yourcompany.credittracker.service;

import com.yourcompany.credittracker.dto.BalanceRow;
import com.yourcompany.credittracker.dto.ProductBalanceSummary;
import com.yourcompany.credittracker.dto.TransactionRequest;
import com.yourcompany.credittracker.model.*;
import com.yourcompany.credittracker.repository.CreditTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreditService {
    private final CreditTransactionRepository transactionRepository;
    private final CustomerService customerService;
    private final ProductService productService;

    @Transactional
    public CreditTransaction addTransaction(Long customerId, TransactionRequest request, String createdBy) {
        Customer customer = customerService.get(customerId);
        Product product = productService.get(request.productId());
        ProductPrice currentPrice = productService.currentPrice(product);

        CreditTransaction tx = new CreditTransaction();
        tx.setCustomer(customer);
        tx.setProduct(product);
        tx.setTransactionType(request.transactionType());
        tx.setNotes(request.notes());
        tx.setCreatedBy(createdBy);
        tx.setTransactionDate(LocalDateTime.now());

        if (request.transactionType() == TransactionType.PURCHASE) {
            if (request.units() == null || request.units().signum() <= 0) {
                throw new IllegalArgumentException("Credits to add must be greater than zero");
            }
            BigDecimal units = normalizeUnits(request.units());
            if (units.signum() <= 0) {
                throw new IllegalArgumentException("Credits to add must be greater than zero");
            }
            tx.setUnits(units);
            tx.setPricePerUnitAtTime(currentPrice.getPricePerUnit());
        } else {
            if (request.units() == null || request.units().compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException("Units must be non-zero");
            }
            BigDecimal units = request.units();
            if (request.transactionType() == TransactionType.CONSUMPTION && units.signum() > 0) {
                units = units.negate();
            }
            units = normalizeUnits(units);
            if (units.compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException("Units must be non-zero");
            }
            tx.setUnits(units);
            tx.setPricePerUnitAtTime(currentPrice.getPricePerUnit());
        }
        return transactionRepository.save(tx);
    }

    private BigDecimal normalizeUnits(BigDecimal units) {
        return units.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public List<BalanceRow> balances(Long customerId) {
        Customer customer = customerService.get(customerId);
        Map<Long, ProductBalanceSummary> summaries = transactionRepository.summarizeBalancesByCustomer(customer).stream()
                .collect(Collectors.toMap(ProductBalanceSummary::productId, Function.identity()));

        return productService.all().stream()
                .filter(product -> {
                    ProductBalanceSummary summary = summaries.get(product.getId());
                    BigDecimal balance = summary == null ? BigDecimal.ZERO : summary.balance();
                    return product.isActive() || balance.signum() != 0;
                })
                .map(product -> {
                    ProductBalanceSummary summary = summaries.get(product.getId());
                    BigDecimal balance = summary == null ? BigDecimal.ZERO : summary.balance();
                    LocalDateTime last = summary == null ? null : summary.lastTransactionDate();
                    return new BalanceRow(product.getId(), product.getName(), product.getUnitLabel(),
                            productService.currentPriceValue(product), balance, product.getColorHexCode(), last);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<CreditTransaction> history(Long customerId, Pageable pageable) {
        return transactionRepository.findByCustomerOrderByTransactionDateDesc(customerService.get(customerId), pageable);
    }

    @Transactional(readOnly = true)
    public CreditTransaction lastTransaction(Long customerId) {
        return transactionRepository.findTopByCustomerOrderByTransactionDateDesc(customerService.get(customerId));
    }
}
