package com.yourcompany.credittracker.repository;

import com.yourcompany.credittracker.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByActiveTrueOrderByNameAsc();
    List<Product> findAllByOrderByNameAsc();
    Optional<Product> findByNameIgnoreCase(String name);
}
