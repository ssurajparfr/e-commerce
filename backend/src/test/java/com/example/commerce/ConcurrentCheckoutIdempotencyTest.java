package com.example.commerce;

import com.example.commerce.cart.CartService;
import com.example.commerce.catalog.Product;
import com.example.commerce.catalog.ProductRepository;
import com.example.commerce.checkout.CheckoutService;
import com.example.commerce.inventory.Inventory;
import com.example.commerce.inventory.InventoryRepository;
import com.example.commerce.orders.OrderRepository;
import com.example.commerce.payments.PaymentAttemptRepository;
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
import java.util.concurrent.Future;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class ConcurrentCheckoutIdempotencyTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    ProductRepository productRepository;
    @Autowired
    InventoryRepository inventoryRepository;
    @Autowired
    CartService cartService;
    @Autowired
    CheckoutService checkoutService;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    PaymentAttemptRepository paymentAttemptRepository;

    @Test
    void concurrentSameKeyCreatesOneOrder() throws Exception {
        Product p = new Product();
        p.setSku("CC-1");
        p.setName("Cc");
        p.setPriceCents(1000);
        p.setActive(true);
        productRepository.save(p);

        Inventory inv = new Inventory();
        inv.setProductId(p.getId());
        inv.setAvailableQuantity(5);
        inv.setReservedQuantity(0);
        inventoryRepository.save(inv);

        String cookie = "cookie-cc";
        cartService.addItem(cookie, p.getId(), 1);

        String key = "cc-key";
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflictOrSuccess = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futures.add(pool.submit(() -> {
                try {
                    start.await();
                    System.out.println("THREAD START " + Thread.currentThread().getName());
                    checkoutService.checkout(cookie, key, "corr", "trace");
                    System.out.println("THREAD DONE " + Thread.currentThread().getName());
                    success.incrementAndGet();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            try { f.get(10, java.util.concurrent.TimeUnit.SECONDS); } catch (Exception e) { System.out.println("FUTURE ERROR: " + e); throw new RuntimeException(e); }
        }
        pool.shutdown();
        while (!pool.isTerminated()) Thread.yield();

        assertThat(success.get()).isEqualTo(2);
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(paymentAttemptRepository.count()).isEqualTo(1);
        assertThat(inventoryRepository.findByProductId(p.getId()).get().getReservedQuantity()).isEqualTo(1);
    }
}
