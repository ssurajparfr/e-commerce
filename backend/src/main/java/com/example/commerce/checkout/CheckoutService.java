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
    private final PaymentService paymentService;
    private final int vatRateBasisPoints;

    public CheckoutService(CartService cartService, ProductRepository productRepository,
                           InventoryService inventoryService, OrderRepository orderRepository,
                           PaymentService paymentService,
                           @Value("${commerce.vat-rate:0.20}") String vatRate) {
        this.cartService = cartService;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
        this.vatRateBasisPoints = new BigDecimal(vatRate).movePointRight(4).intValue();
    }

    @Transactional
    public Order checkout(String cookieId, String idempotencyKey, String correlationId, String traceId) {
        Cart cart = cartService.get(cookieId);
        if (cart == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }
        String requestHash = hash(cookieId + cart.getItems().size() + cart.getItems().stream().map(i -> i.getProductId()+":"+i.getQuantity()).reduce("", (a,b)->a+b));
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                Order o = existing.get();
                if (o.getCheckoutRequestHash() != null && !o.getCheckoutRequestHash().equals(requestHash)) {
                    throw new IdempotencyConflictException("Idempotency key reused with different payload");
                }
                return o;
            }
        }

        Order order = new Order();
        order.setGuestRef(cookieId);
        order.setIdempotencyKey(idempotencyKey);
        order.setCheckoutRequestHash(requestHash);
        order.setCorrelationId(correlationId);
        order.setTraceId(traceId);
        order.setVatRateBasisPoints(vatRateBasisPoints);

        long subtotal = 0;
        long vatTotal = 0;
        for (var ci : cart.getItems()) {
            Product product = productRepository.findById(ci.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));
            if (!product.isActive()) {
                throw new IllegalArgumentException("Product not active");
            }
            int qty = ci.getQuantity();
            long unitPrice = product.getPriceCents();
            long lineSubtotal = unitPrice * qty;
            long lineVat = (lineSubtotal * vatRateBasisPoints) / 10000;
            long lineTotal = lineSubtotal + lineVat;
            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setDescription(product.getName());
            item.setUnitPriceCents(unitPrice);
            item.setVatRateBasisPoints(vatRateBasisPoints);
            item.setQuantity(qty);
            item.setLineTotalCents(lineTotal);
            order.addItem(item);
            subtotal += lineSubtotal;
            vatTotal += lineVat;
        }
        order.setSubtotalCents(subtotal);
        order.setVatCents(vatTotal);
        order.setTotalCents(subtotal + vatTotal);
        orderRepository.save(order);

        try {
            for (var ci : cart.getItems()) {
                inventoryService.reserve(ci.getProductId(), ci.getQuantity());
            }
            order.transitionTo(OrderStatus.INVENTORY_RESERVED);
            orderRepository.save(order);
        } catch (OutOfStockException e) {
            order.transitionTo(OrderStatus.INVENTORY_FAILED);
            orderRepository.save(order);
            throw e;
        }

        order.transitionTo(OrderStatus.PAYMENT_PENDING);
        orderRepository.save(order);

        String paymentKey = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey + "-payment" : "payment-" + order.getId();
        PaymentResult result = paymentService.charge(order, order.getTotalCents(), paymentKey, requestHash);

        if ("SUCCESS".equals(result.status())) {
            order.setPaymentStatus(PaymentStatus.SUCCESS);
            order.transitionTo(OrderStatus.CONFIRMED);
        } else {
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.transitionTo(OrderStatus.PAYMENT_FAILED);
            for (var ci : cart.getItems()) {
                inventoryService.release(ci.getProductId(), ci.getQuantity());
            }
        }
        orderRepository.save(order);
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
