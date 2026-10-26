package com.example.commerce.orders;

public enum OrderStatus {
    CREATED,
    INVENTORY_RESERVED,
    PAYMENT_PENDING,
    CONFIRMED,
    FULFILLMENT_PENDING,
    SHIPPED,
    DELIVERED,
    INVENTORY_FAILED,
    PAYMENT_FAILED,
    CANCELLED,
    EXPIRED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case CREATED -> next == INVENTORY_RESERVED || next == INVENTORY_FAILED || next == CANCELLED || next == EXPIRED;
            case INVENTORY_RESERVED -> next == PAYMENT_PENDING || next == CANCELLED || next == EXPIRED;
            case PAYMENT_PENDING -> next == CONFIRMED || next == PAYMENT_FAILED || next == CANCELLED || next == EXPIRED;
            case CONFIRMED -> next == FULFILLMENT_PENDING || next == CANCELLED;
            case FULFILLMENT_PENDING -> next == SHIPPED || next == CANCELLED;
            case SHIPPED -> next == DELIVERED;
            default -> false;
        };
    }
}
