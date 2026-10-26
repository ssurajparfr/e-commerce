package com.example.commerce;

import com.example.commerce.catalog.Product;
import com.example.commerce.catalog.ProductRepository;
import com.example.commerce.inventory.Inventory;
import com.example.commerce.inventory.InventoryRepository;
import com.example.commerce.orders.Order;
import com.example.commerce.orders.OrderRepository;
import com.example.commerce.orders.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ApiValidationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @Value("${local.server.port}")
    int port;

    @Autowired
    ProductRepository productRepository;
    @Autowired
    InventoryRepository inventoryRepository;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;

    private HttpClient client() {
        return HttpClient.newHttpClient();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void checkoutRequiresIdempotencyKey() throws Exception {
        HttpResponse<String> response = client().send(
                HttpRequest.newBuilder(URI.create(url("/api/checkout")))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"error\":\"BAD_REQUEST\"");
    }

    @Test
    void cartItemRequiresProductId() throws Exception {
        HttpResponse<String> response = client().send(
                HttpRequest.newBuilder(URI.create(url("/api/cart/items")))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"error\":\"BAD_REQUEST\"");
    }

    @Test
    void orderRequiresAccess() throws Exception {
        Order order = new Order();
        order.setGuestRef("owner-cookie");
        order.setPaymentStatus(com.example.commerce.orders.PaymentStatus.PENDING);
        order.setSubtotalCents(1000);
        order.setVatCents(200);
        order.setTotalCents(1200);
        order.setVatRateBasisPoints(2000);
        orderRepository.saveAndFlush(order);

        HttpResponse<String> noCookie = client().send(
                HttpRequest.newBuilder(URI.create(url("/api/orders/" + order.getId())))
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(noCookie.statusCode()).isEqualTo(403);

        HttpResponse<String> wrongCookie = client().send(
                HttpRequest.newBuilder(URI.create(url("/api/orders/" + order.getId())))
                        .header("Cookie", "cart_id=wrong-cookie")
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(wrongCookie.statusCode()).isEqualTo(403);

        HttpResponse<String> rightCookie = client().send(
                HttpRequest.newBuilder(URI.create(url("/api/orders/" + order.getId())))
                        .header("Cookie", "cart_id=owner-cookie")
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(rightCookie.statusCode()).isEqualTo(200);
    }

    @Test
    void checkoutOutOfStockReturns409() throws Exception {
        Product product = new Product();
        product.setSku("OOS-" + UUID.randomUUID());
        product.setName("OOS");
        product.setPriceCents(1000);
        product.setActive(true);
        productRepository.save(product);

        Inventory inventory = new Inventory();
        inventory.setProductId(product.getId());
        inventory.setAvailableQuantity(0);
        inventory.setReservedQuantity(0);
        inventoryRepository.save(inventory);

        String cookie = UUID.randomUUID().toString();
        client().send(
                HttpRequest.newBuilder(URI.create(url("/api/cart/items")))
                        .header("Content-Type", "application/json")
                        .header("Cookie", "cart_id=" + cookie)
                        .POST(HttpRequest.BodyPublishers.ofString("{\"productId\":" + product.getId() + ",\"quantity\":1}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        HttpResponse<String> response = client().send(
                HttpRequest.newBuilder(URI.create(url("/api/checkout")))
                        .header("Content-Type", "application/json")
                        .header("Cookie", "cart_id=" + cookie)
                        .header("Idempotency-Key", "oos-key")
                        .POST(HttpRequest.BodyPublishers.ofString("{}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("\"error\":\"OUT_OF_STOCK\"");
    }

    @Test
    void checkoutIdempotencyConflictReturns409() throws Exception {
        Product product = new Product();
        product.setSku("CONF-" + UUID.randomUUID());
        product.setName("Conf");
        product.setPriceCents(1000);
        product.setActive(true);
        productRepository.save(product);

        Inventory inventory = new Inventory();
        inventory.setProductId(product.getId());
        inventory.setAvailableQuantity(5);
        inventory.setReservedQuantity(0);
        inventoryRepository.save(inventory);

        String cookie = UUID.randomUUID().toString();
        client().send(
                HttpRequest.newBuilder(URI.create(url("/api/cart/items")))
                        .header("Content-Type", "application/json")
                        .header("Cookie", "cart_id=" + cookie)
                        .POST(HttpRequest.BodyPublishers.ofString("{\"productId\":" + product.getId() + ",\"quantity\":1}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        HttpResponse<String> first = client().send(
                HttpRequest.newBuilder(URI.create(url("/api/checkout")))
                        .header("Content-Type", "application/json")
                        .header("Cookie", "cart_id=" + cookie)
                        .header("Idempotency-Key", "conf-key")
                        .POST(HttpRequest.BodyPublishers.ofString("{}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(first.statusCode()).isEqualTo(200);

        // change cart payload (quantity 2) while reusing key
        client().send(
                HttpRequest.newBuilder(URI.create(url("/api/cart/items")))
                        .header("Content-Type", "application/json")
                        .header("Cookie", "cart_id=" + cookie)
                        .POST(HttpRequest.BodyPublishers.ofString("{\"productId\":" + product.getId() + ",\"quantity\":1}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        HttpResponse<String> second = client().send(
                HttpRequest.newBuilder(URI.create(url("/api/checkout")))
                        .header("Content-Type", "application/json")
                        .header("Cookie", "cart_id=" + cookie)
                        .header("Idempotency-Key", "conf-key")
                        .POST(HttpRequest.BodyPublishers.ofString("{}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(second.statusCode()).isEqualTo(409);
        assertThat(second.body()).contains("\"error\":\"IDEMPOTENCY_CONFLICT\"");
    }

    @Test
    void invalidOrderStatusUpdateRejectedByTrigger() {
        Order order = new Order();
        order.setGuestRef("cookie");
        order.setPaymentStatus(com.example.commerce.orders.PaymentStatus.PENDING);
        order.setSubtotalCents(1000);
        order.setVatCents(200);
        order.setTotalCents(1200);
        order.setVatRateBasisPoints(2000);
        orderRepository.saveAndFlush(order);

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE orders SET status = 'CONFIRMED' WHERE id = ?", order.getId()))
                .isInstanceOf(Exception.class);
    }
}
