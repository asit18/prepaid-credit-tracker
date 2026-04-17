package com.yourcompany.credittracker.service;

import com.yourcompany.credittracker.dto.PriceRequest;
import com.yourcompany.credittracker.dto.ProductRequest;
import com.yourcompany.credittracker.exception.NotFoundException;
import com.yourcompany.credittracker.model.Product;
import com.yourcompany.credittracker.model.ProductPrice;
import com.yourcompany.credittracker.repository.ProductPriceRepository;
import com.yourcompany.credittracker.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductPriceRepository productPriceRepository;

    @Transactional(readOnly = true)
    public List<Product> all() {
        return productRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Product> active() {
        return productRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Product get(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product not found"));
    }

    @Transactional
    public Product create(ProductRequest request) {
        ensureUniqueName(request.name(), null);
        Product product = new Product();
        apply(product, request);
        return productRepository.save(product);
    }

    @Transactional
    public Product createWithInitialPrice(ProductRequest request, PriceRequest priceRequest, String createdBy) {
        Product product = create(request);
        ProductPrice price = new ProductPrice();
        price.setProduct(product);
        price.setPricePerUnit(normalizePrice(priceRequest.pricePerUnit()));
        price.setEffectiveFrom(priceRequest.effectiveFrom());
        price.setCreatedBy(createdBy);
        productPriceRepository.save(price);
        return product;
    }

    @Transactional
    public Product update(Long id, ProductRequest request) {
        Product product = get(id);
        ensureUniqueName(request.name(), id);
        apply(product, request);
        return product;
    }

    @Transactional
    public Product deactivate(Long id) {
        Product product = get(id);
        product.setActive(false);
        return product;
    }

    @Transactional
    public ProductPrice addPrice(Long productId, PriceRequest request, String createdBy) {
        Product product = get(productId);
        ProductPrice price = new ProductPrice();
        price.setProduct(product);
        price.setPricePerUnit(normalizePrice(request.pricePerUnit()));
        price.setEffectiveFrom(request.effectiveFrom());
        price.setCreatedBy(createdBy);
        return productPriceRepository.save(price);
    }

    @Transactional(readOnly = true)
    public ProductPrice currentPrice(Product product) {
        return productPriceRepository.findFirstByProductAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(product, LocalDate.now())
                .orElseThrow(() -> new IllegalStateException("No current price configured for " + product.getName()));
    }

    @Transactional(readOnly = true)
    public BigDecimal currentPriceValue(Product product) {
        return currentPrice(product).getPricePerUnit();
    }

    @Transactional(readOnly = true)
    public List<ProductPrice> priceHistory(Long productId) {
        return productPriceRepository.findByProductOrderByEffectiveFromDesc(get(productId));
    }

    private void apply(Product product, ProductRequest request) {
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setUnitLabel(request.unitLabel().trim());
        product.setActive(request.active());
    }

    private void ensureUniqueName(String name, Long currentProductId) {
        String normalized = name == null ? "" : name.trim();
        productRepository.findByNameIgnoreCase(normalized).ifPresent(existing -> {
            if (currentProductId == null || !existing.getId().equals(currentProductId)) {
                throw new IllegalArgumentException("Product name already exists");
            }
        });
    }

    private BigDecimal normalizePrice(BigDecimal price) {
        return price.setScale(2, RoundingMode.HALF_UP);
    }
}
