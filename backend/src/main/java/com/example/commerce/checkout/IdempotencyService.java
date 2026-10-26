package com.example.commerce.checkout;

import com.example.commerce.cart.Cart;
import com.example.commerce.catalog.Product;
import com.example.commerce.catalog.ProductRepository;
import com.example.commerce.orders.Order;
import com.example.commerce.orders.OrderItem;
import com.example.commerce.orders.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class IdempotencyService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final int vatRateBasisPoints;

    @PersistenceContext
    private EntityManager entityManager;

    public IdempotencyService(OrderRepository orderRepository, ProductRepository productRepository,
                             @Value("${commerce.vat-rate:0.20}") String vatRate) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.vatRateBasisPoints = new BigDecimal(vatRate).movePointRight(4).intValue();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order reserveIdempotentOrder(String cookieId, String idempotencyKey, String requestHash,
                                       String correlationId, String traceId, Cart cart) {
        var existing = orderRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            Order o = existing.get();
            if (o.getCheckoutRequestHash() != null && !o.getCheckoutRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException("Idempotency key reused with different payload");
            }
            return o;
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

        orderRepository.saveAndFlush(order);
        return order;
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Order findExistingOrder(String idempotencyKey) {
        return orderRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
    }
}
