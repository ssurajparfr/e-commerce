package com.example.commerce.payments;

import com.example.commerce.orders.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private final PaymentAttemptRepository repository;
    private final PaymentProvider provider;

    public PaymentService(PaymentAttemptRepository repository, PaymentProvider provider) {
        this.repository = repository;
        this.provider = provider;
    }

    @Transactional
    public PaymentResult charge(Order order, long amountCents, String idempotencyKey, String requestHash) {
        var existing = repository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            PaymentAttempt attempt = existing.get();
            if (!attempt.getRequestHash().equals(requestHash)) {
                throw new PaymentIdempotencyConflictException("Idempotency key reused with different payload");
            }
            return new PaymentResult(attempt.getStatus(), attempt.getProviderReference(), attempt.getFailureCode());
        }
        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setOrder(order);
        attempt.setIdempotencyKey(idempotencyKey);
        attempt.setRequestHash(requestHash);
        attempt.setAmountCents(amountCents);
        attempt.setStatus("PENDING");
        repository.save(attempt);

        PaymentResult result = provider.charge(new PaymentRequest(idempotencyKey, requestHash, amountCents, order.getCurrency(), order.getId()));
        attempt.setStatus(result.status());
        attempt.setProviderReference(result.providerReference());
        attempt.setFailureCode(result.failureCode());
        repository.save(attempt);
        return result;
    }
}
