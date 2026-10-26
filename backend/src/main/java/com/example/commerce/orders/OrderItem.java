package com.example.commerce.orders;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    @Column(name = "product_id")
    private Long productId;

    @Column(nullable = false)
    private String description;

    @Column(name = "unit_price_cents", nullable = false)
    private long unitPriceCents;

    @Column(name = "vat_rate_basis_points", nullable = false)
    private int vatRateBasisPoints;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "line_total_cents", nullable = false)
    private long lineTotalCents;

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getUnitPriceCents() { return unitPriceCents; }
    public void setUnitPriceCents(long unitPriceCents) { this.unitPriceCents = unitPriceCents; }
    public int getVatRateBasisPoints() { return vatRateBasisPoints; }
    public void setVatRateBasisPoints(int vatRateBasisPoints) { this.vatRateBasisPoints = vatRateBasisPoints; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public long getLineTotalCents() { return lineTotalCents; }
    public void setLineTotalCents(long lineTotalCents) { this.lineTotalCents = lineTotalCents; }
}
