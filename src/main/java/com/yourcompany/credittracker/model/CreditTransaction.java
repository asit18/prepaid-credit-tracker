package com.yourcompany.credittracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "credit_transactions", indexes = {
        @Index(name = "idx_credit_customer_product_date", columnList = "customer_id,product_id,transaction_date"),
        @Index(name = "idx_credit_transaction_date", columnList = "transaction_date"),
        @Index(name = "idx_credit_transaction_type", columnList = "transaction_type")
})
public class CreditTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal units;

    @Column(name = "amount_paid", precision = 10, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "price_per_unit_at_time", precision = 10, scale = 2)
    private BigDecimal pricePerUnitAtTime;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate = LocalDateTime.now();

    @Column(name = "created_by", nullable = false)
    private String createdBy;
}
