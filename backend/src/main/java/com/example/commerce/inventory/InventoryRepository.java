package com.example.commerce.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByProductId(Long productId);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("select i from Inventory i where i.productId = :productId")
    Optional<Inventory> findLockedByProductId(Long productId);
}
