package com.example.commerce.payments;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MockPaymentProvider implements PaymentProvider {
    private final Map<String, PaymentResult> results = new ConcurrentHashMap<>();
    private final Map<String, String> hashes = new ConcurrentHashMap<>();

    @Override
    public PaymentResult charge(PaymentRequest request) {
        String existingHash = hashes.get(request.idempotencyKey());
        if (existingHash != null) {
            if (!existingHash.equals(request.payloadHash())) {
                throw new PaymentIdempotencyConflictException("Idempotency key reused with different payload");
            }
            return results.get(request.idempotencyKey());
        }
        hashes.put(request.idempotencyKey(), request.payloadHash());
        PaymentResult result = PaymentResult.success("mock-" + request.idempotencyKey());
        results.put(request.idempotencyKey(), result);
        return result;
    }
}
