package com.example.commerce;

import com.example.commerce.cart.CartService;
import com.example.commerce.catalog.Product;
import com.example.commerce.catalog.ProductRepository;
import com.example.commerce.checkout.CheckoutService;
import com.example.commerce.inventory.Inventory;
import com.example.commerce.inventory.InventoryRepository;
import com.example.commerce.orders.Order;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class CheckoutIdempotencyTest {

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
    @Autowired CartService cartService;
    @Autowired CheckoutService checkoutService;
    @Autowired OrderRepository orderRepository;
    @Autowired PaymentAttemptRepository paymentAttemptRepository;

    @Test
    void duplicateCheckoutCreatesOneOrder() {
        Product p = new Product();
        p.setSku("IDEM-1");
        p.setName("Idem");
        p.setPriceCents(1000);
        p.setActive(true);
        productRepository.save(p);

        Inventory inv = new Inventory();
        inv.setProductId(p.getId());
        inv.setAvailableQuantity(5);
        inv.setReservedQuantity(0);
        inventoryRepository.save(inv);

        String cookie = "cookie-idem";
        cartService.addItem(cookie, p.getId(), 1);

        String key = "idem-key-1";
        Order first = checkoutService.checkout(cookie, key, "corr", "trace");
        Order second = checkoutService.checkout(cookie, key, "corr", "trace");

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(paymentAttemptRepository.count()).isEqualTo(1);
        assertThat(inventoryRepository.findByProductId(p.getId()).get().getReservedQuantity()).isEqualTo(1);
    }
}
