package com.example.commerce;

import com.example.commerce.catalog.Product;
import com.example.commerce.catalog.ProductRepository;
import com.example.commerce.inventory.Inventory;
import com.example.commerce.inventory.InventoryRepository;
import com.example.commerce.inventory.InventoryService;
import com.example.commerce.inventory.OutOfStockException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class InventoryReservationConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired ProductRepository productRepository;
    @Autowired InventoryRepository inventoryRepository;
    @Autowired InventoryService inventoryService;

    @Test
    void lastUnitGivesOneSuccess() throws Exception {
        Product p = new Product();
        p.setSku("RACE-1");
        p.setName("Race");
        p.setPriceCents(1000);
        p.setActive(true);
        productRepository.save(p);

        Inventory inv = new Inventory();
        inv.setProductId(p.getId());
        inv.setAvailableQuantity(1);
        inv.setReservedQuantity(0);
        inventoryRepository.save(inv);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        for (int i = 0; i < 2; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    inventoryService.reserve(p.getId(), 1);
                    success.incrementAndGet();
                } catch (com.example.commerce.inventory.OutOfStockException e) {
                    failure.incrementAndGet();
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            });
        }
        start.countDown();
        pool.shutdown();
        while (!pool.isTerminated()) Thread.yield();

        assertThat(success.get()).isEqualTo(1);
        assertThat(failure.get()).isEqualTo(1);
    }
}
