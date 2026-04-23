package com.yourcompany.credittracker.service;

import com.yourcompany.credittracker.dto.BalanceRow;
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
import java.util.Comparator;
import java.util.List;

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
            BigDecimal units = wholeUnits(request.units());
            if (units.signum() <= 0) {
                throw new IllegalArgumentException("Credits to add must round down to at least one whole credit");
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
            units = wholeUnits(units);
            if (units.compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException("Units must round down to at least one whole credit");
            }
            tx.setUnits(units);
            tx.setPricePerUnitAtTime(currentPrice.getPricePerUnit());
        }
        return transactionRepository.save(tx);
    }

    private BigDecimal wholeUnits(BigDecimal units) {
        BigDecimal wholeMagnitude = units.abs().setScale(0, RoundingMode.FLOOR);
        return units.signum() < 0 ? wholeMagnitude.negate() : wholeMagnitude;
    }

    @Transactional(readOnly = true)
    public List<BalanceRow> balances(Long customerId) {
        Customer customer = customerService.get(customerId);
        return productService.active().stream().map(product -> {
            BigDecimal balance = transactionRepository.balanceFor(customer, product);
            LocalDateTime last = transactionRepository.findByCustomerOrderByTransactionDateDesc(customer, Pageable.unpaged())
                    .stream()
                    .filter(t -> t.getProduct().getId().equals(product.getId()))
                    .map(CreditTransaction::getTransactionDate)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
            return new BalanceRow(product.getId(), product.getName(), product.getUnitLabel(),
                    productService.currentPriceValue(product), balance, product.getColorHexCode(), last);
        }).toList();
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
