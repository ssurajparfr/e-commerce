package com.example.commerce.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {
    private final ProductRepository repository;

    public CatalogService(ProductRepository repository) {
        this.repository = repository;
    }

    public Page<Product> list(Pageable pageable) {
        return repository.findByActiveTrue(pageable);
    }

    public Product get(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }
}
