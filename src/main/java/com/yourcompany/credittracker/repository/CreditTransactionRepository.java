package com.yourcompany.credittracker.repository;

import com.yourcompany.credittracker.dto.CreditsBalanceReportRow;
import com.yourcompany.credittracker.dto.ProductBalanceSummary;
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

    @Query("""
            select new com.yourcompany.credittracker.dto.ProductBalanceSummary(
                t.product.id,
                coalesce(sum(t.units), 0),
                max(t.transactionDate)
            )
            from CreditTransaction t
            where t.customer = :customer
            group by t.product.id
            """)
    List<ProductBalanceSummary> summarizeBalancesByCustomer(@Param("customer") Customer customer);

    @Query("select coalesce(sum(t.units), 0) from CreditTransaction t where t.transactionDate >= :from and t.transactionDate < :to and t.transactionType = :type")
    BigDecimal sumUnitsByTypeBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("type") TransactionType type);

    @Query("""
            select new com.yourcompany.credittracker.dto.CreditsBalanceReportRow(
                t.customer.name,
                t.product.name,
                coalesce(sum(t.units), 0)
            )
            from CreditTransaction t
            where t.transactionDate < :toExclusive
            group by t.customer.id, t.customer.name, t.product.id, t.product.name
            having coalesce(sum(t.units), 0) <> 0
            order by t.customer.name asc, t.product.name asc
            """)
    List<CreditsBalanceReportRow> creditsBalanceReport(@Param("toExclusive") LocalDateTime toExclusive);

}
