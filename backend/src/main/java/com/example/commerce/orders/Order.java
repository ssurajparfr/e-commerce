package com.example.commerce.orders;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guest_ref")
    private String guestRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "subtotal_cents", nullable = false)
    private long subtotalCents;

    @Column(name = "vat_cents", nullable = false)
    private long vatCents;

    @Column(name = "total_cents", nullable = false)
    private long totalCents;

    @Column(nullable = false)
    private String currency = "EUR";

    @Column(name = "vat_rate_basis_points", nullable = false)
    private int vatRateBasisPoints;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "checkout_request_hash")
    private String checkoutRequestHash;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "trace_id")
    private String traceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    public Long getId() { return id; }
    public String getGuestRef() { return guestRef; }
    public void setGuestRef(String guestRef) { this.guestRef = guestRef; }
    public OrderStatus getStatus() { return status; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public long getSubtotalCents() { return subtotalCents; }
    public void setSubtotalCents(long subtotalCents) { this.subtotalCents = subtotalCents; }
    public long getVatCents() { return vatCents; }
    public void setVatCents(long vatCents) { this.vatCents = vatCents; }
    public long getTotalCents() { return totalCents; }
    public void setTotalCents(long totalCents) { this.totalCents = totalCents; }
    public String getCurrency() { return currency; }
    public int getVatRateBasisPoints() { return vatRateBasisPoints; }
    public void setVatRateBasisPoints(int vatRateBasisPoints) { this.vatRateBasisPoints = vatRateBasisPoints; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getCheckoutRequestHash() { return checkoutRequestHash; }
    public void setCheckoutRequestHash(String checkoutRequestHash) { this.checkoutRequestHash = checkoutRequestHash; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderItem> getItems() { return items; }

    public void transitionTo(OrderStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new IllegalStateException("Invalid order transition " + status + " -> " + next);
        }
        this.status = next;
        this.updatedAt = Instant.now();
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }
}
