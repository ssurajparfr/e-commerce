package com.example.commerce.payments;

public record PaymentRequest(String idempotencyKey, String payloadHash, long amountCents, String currency, Long orderId) {}
