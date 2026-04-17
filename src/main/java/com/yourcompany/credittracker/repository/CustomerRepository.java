package com.yourcompany.credittracker.repository;

import com.yourcompany.credittracker.model.Customer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    @Override
    @EntityGraph(attributePaths = "contacts")
    Optional<Customer> findById(Long id);

    List<Customer> findTop10ByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(String search);
    List<Customer> findByActiveTrueOrderByNameAsc();
    Optional<Customer> findByNameIgnoreCaseAndPhone(String name, String phone);
}
