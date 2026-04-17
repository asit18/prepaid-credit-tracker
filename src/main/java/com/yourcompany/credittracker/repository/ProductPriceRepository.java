package com.yourcompany.credittracker.repository;

import com.yourcompany.credittracker.model.Product;
import com.yourcompany.credittracker.model.ProductPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductPriceRepository extends JpaRepository<ProductPrice, Long> {
    Optional<ProductPrice> findFirstByProductAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(Product product, LocalDate date);
    List<ProductPrice> findByProductOrderByEffectiveFromDesc(Product product);
}
