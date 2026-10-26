package com.example.commerce.inventory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
    private final InventoryRepository repository;

    public InventoryService(InventoryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void reserve(Long productId, int quantity) {
        Inventory inventory = repository.findLockedByProductId(productId)
                .orElseThrow(() -> new OutOfStockException("No inventory for product " + productId));
        if (inventory.getAvailableQuantity() < quantity) {
            throw new OutOfStockException("Insufficient stock for product " + productId);
        }
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() - quantity);
        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        repository.save(inventory);
    }

    @Transactional
    public void release(Long productId, int quantity) {
        Inventory inventory = repository.findByProductId(productId).orElse(null);
        if (inventory == null) return;
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() + quantity);
        inventory.setReservedQuantity(Math.max(0, inventory.getReservedQuantity() - quantity));
        repository.save(inventory);
    }
}
