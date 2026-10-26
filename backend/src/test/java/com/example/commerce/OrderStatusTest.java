package com.example.commerce;

import com.example.commerce.orders.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {
    @Test
    void validTransitions() {
        assertThat(OrderStatus.CREATED.canTransitionTo(OrderStatus.INVENTORY_RESERVED)).isTrue();
        assertThat(OrderStatus.PAYMENT_PENDING.canTransitionTo(OrderStatus.CONFIRMED)).isTrue();
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.FULFILLMENT_PENDING)).isTrue();
    }

    @Test
    void invalidTransitions() {
        assertThat(OrderStatus.CREATED.canTransitionTo(OrderStatus.CONFIRMED)).isFalse();
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CREATED)).isFalse();
        assertThat(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.SHIPPED)).isFalse();
    }
}
