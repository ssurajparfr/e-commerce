package com.example.commerce;

import com.example.commerce.payments.MockPaymentProvider;
import com.example.commerce.payments.PaymentRequest;
import com.example.commerce.payments.PaymentResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockPaymentIdempotencyTest {
    @Test
    void sameKeySamePayloadReturnsSameResult() {
        MockPaymentProvider provider = new MockPaymentProvider();
        PaymentRequest req = new PaymentRequest("key-1", "hash-a", 1000, "EUR", 1L);
        PaymentResult first = provider.charge(req);
        PaymentResult second = provider.charge(req);
        assertThat(second).isEqualTo(first);
    }

    @Test
    void sameKeyDifferentPayloadConflicts() {
        MockPaymentProvider provider = new MockPaymentProvider();
        provider.charge(new PaymentRequest("key-2", "hash-a", 1000, "EUR", 1L));
        assertThatThrownBy(() -> provider.charge(new PaymentRequest("key-2", "hash-b", 1000, "EUR", 1L)))
                .isInstanceOf(com.example.commerce.payments.PaymentIdempotencyConflictException.class);
    }
}
