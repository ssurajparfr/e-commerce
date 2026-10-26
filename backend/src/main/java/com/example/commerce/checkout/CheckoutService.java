package com.example.commerce.checkout;

import com.example.commerce.cart.Cart;
import com.example.commerce.cart.CartService;
import com.example.commerce.catalog.Product;
import com.example.commerce.catalog.ProductRepository;
import com.example.commerce.inventory.InventoryService;
import com.example.commerce.inventory.OutOfStockException;
import com.example.commerce.orders.Order;
import com.example.commerce.orders.OrderItem;
import com.example.commerce.orders.OrderRepository;
import com.example.commerce.orders.OrderStatus;
import com.example.commerce.orders.PaymentStatus;
import com.example.commerce.payments.PaymentResult;
import com.example.commerce.payments.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class CheckoutService {
    private final CartService cartService;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final OrderRepository orderRepository;
    private final IdempotencyService idempotencyService;
    private final PaymentService paymentService;
    private final int vatRateBasisPoints;

    public CheckoutService(CartService cartService, ProductRepository productRepository,
                           InventoryService inventoryService, OrderRepository orderRepository,
                           IdempotencyService idempotencyService, PaymentService paymentService,
                           @Value("${commerce.vat-rate:0.20}") String vatRate) {
        this.cartService = cartService;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.orderRepository = orderRepository;
        this.idempotencyService = idempotencyService;
        this.paymentService = paymentService;
        this.vatRateBasisPoints = new BigDecimal(vatRate).movePointRight(4).intValue();
    }

    @Transactional
    public Order checkout(String cookieId, String idempotencyKey, String correlationId, String traceId) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }
        Cart cart = cartService.get(cookieId);
        if (cart == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }
        String requestHash = hash(cookieId + cart.getItems().size() + cart.getItems().stream().map(i -> i.getProductId()+":"+i.getQuantity()).reduce("", (a,b)->a+b));

        Order order;
        try {
            order = idempotencyService.reserveIdempotentOrder(cookieId, idempotencyKey, requestHash, correlationId, traceId, cart);
        } catch (DataIntegrityViolationException e) {
            Order existing = idempotencyService.findExistingOrder(idempotencyKey);
            if (existing == null) {
                throw e;
            }
            if (existing.getCheckoutRequestHash() != null && !existing.getCheckoutRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException("Idempotency key reused with different payload");
            }
            return existing;
        }
        // Existing order: do not reprocess inventory or payment.
        if (order.getStatus() != OrderStatus.CREATED) {
            return order;
        }

        try {
            for (var ci : cart.getItems()) {
                inventoryService.reserve(ci.getProductId(), ci.getQuantity());
            }
            order.transitionTo(OrderStatus.INVENTORY_RESERVED);
            orderRepository.saveAndFlush(order);
        } catch (OutOfStockException e) {
            throw e;
        }

        order.transitionTo(OrderStatus.PAYMENT_PENDING);
        orderRepository.saveAndFlush(order);

        String paymentKey = idempotencyKey + "-payment";
        com.example.commerce.payments.PaymentResult result = paymentService.charge(order, order.getTotalCents(), paymentKey, requestHash);

        if ("SUCCESS".equals(result.status())) {
            order.setPaymentStatus(com.example.commerce.orders.PaymentStatus.SUCCESS);
            order.transitionTo(OrderStatus.CONFIRMED);
        } else {
            order.setPaymentStatus(com.example.commerce.orders.PaymentStatus.FAILED);
            order.transitionTo(OrderStatus.PAYMENT_FAILED);
            for (var ci : cart.getItems()) {
                inventoryService.release(ci.getProductId(), ci.getQuantity());
            }
        }
        orderRepository.saveAndFlush(order);
        return order;
    }

    private String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return input;
        }
    }
}
