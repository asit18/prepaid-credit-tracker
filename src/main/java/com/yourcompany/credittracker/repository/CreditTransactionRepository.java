package com.yourcompany.credittracker.repository;

import com.yourcompany.credittracker.model.CreditTransaction;
import com.yourcompany.credittracker.model.Customer;
import com.yourcompany.credittracker.model.Product;
import com.yourcompany.credittracker.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, Long> {
    @EntityGraph(attributePaths = {"product", "customer"})
    Page<CreditTransaction> findByCustomerOrderByTransactionDateDesc(Customer customer, Pageable pageable);

    CreditTransaction findTopByCustomerOrderByTransactionDateDesc(Customer customer);

    @EntityGraph(attributePaths = {"product", "customer"})
    List<CreditTransaction> findAllByOrderByTransactionDateDesc();

    @Query("select coalesce(sum(t.units), 0) from CreditTransaction t where t.customer = :customer and t.product = :product")
    BigDecimal balanceFor(@Param("customer") Customer customer, @Param("product") Product product);

    @Query("select coalesce(sum(t.units), 0) from CreditTransaction t where t.transactionDate >= :from and t.transactionDate < :to and t.transactionType = :type")
    BigDecimal sumUnitsByTypeBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("type") TransactionType type);

}
