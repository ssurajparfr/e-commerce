package com.example.commerce.payments;

public class PaymentIdempotencyConflictException extends RuntimeException {
    public PaymentIdempotencyConflictException(String message) { super(message); }
}
