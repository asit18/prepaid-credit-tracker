package com.yourcompany.credittracker.controller;

import com.yourcompany.credittracker.dto.*;
import com.yourcompany.credittracker.model.*;
import com.yourcompany.credittracker.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApiController {
    private final CustomerService customerService;
    private final ProductService productService;
    private final CreditService creditService;
    private final ReportService reportService;

    @GetMapping("/customers")
    List<CustomerLookupRow> customers(@RequestParam(required = false) String search) {
        return customerService.search(search).stream()
                .map(customer -> new CustomerLookupRow(customer.getId(), customer.getName(), customer.getPhone()))
                .toList();
    }

    @PostMapping("/customers")
    Customer createCustomer(@RequestBody @Valid CustomerRequest request) {
        return customerService.create(request);
    }

    @GetMapping("/customers/{id}")
    Customer customer(@PathVariable Long id) {
        return customerService.get(id);
    }

    @PutMapping("/customers/{id}")
    Customer updateCustomer(@PathVariable Long id, @RequestBody @Valid CustomerRequest request) {
        return customerService.update(id, request);
    }

    @GetMapping("/customers/{id}/balance")
    List<BalanceRow> balance(@PathVariable Long id) {
        return creditService.balances(id);
    }

    @PostMapping("/customers/{id}/transactions")
    CreditTransaction addTransaction(@PathVariable Long id, @RequestBody @Valid TransactionRequest request, Authentication authentication) {
        return creditService.addTransaction(id, request, CurrentUser.email(authentication));
    }

    @GetMapping("/customers/{id}/transactions")
    Page<CreditTransaction> transactions(@PathVariable Long id,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "25") int size) {
        return creditService.history(id, PageRequest.of(page, size));
    }

    @GetMapping("/products")
    List<Product> products() {
        return productService.all();
    }

    @PostMapping("/products")
    Product createProduct(@RequestBody @Valid ProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/products/{id}")
    Product updateProduct(@PathVariable Long id, @RequestBody @Valid ProductRequest request) {
        return productService.update(id, request);
    }

    @PostMapping("/products/{id}/prices")
    ProductPrice addPrice(@PathVariable Long id, @RequestBody @Valid PriceRequest request, Authentication authentication) {
        return productService.addPrice(id, request, CurrentUser.email(authentication));
    }

    @GetMapping("/products/{id}/prices")
    List<ProductPrice> prices(@PathVariable Long id) {
        return productService.priceHistory(id);
    }

    @GetMapping("/reports/transactions")
    List<ReportRow> reportRows(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                               @RequestParam(required = false) Long customerId,
                               @RequestParam(required = false) Long productId,
                               @RequestParam(required = false) TransactionType type) {
        return reportService.rows(from, to, customerId, productId, type);
    }

    @GetMapping("/reports/summary")
    Map<String, ?> reportSummary(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                 @RequestParam(required = false) Long customerId,
                                 @RequestParam(required = false) Long productId,
                                 @RequestParam(required = false) TransactionType type) {
        return reportService.summary(from, to, customerId, productId, type);
    }
}
