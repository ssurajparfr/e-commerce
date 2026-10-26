package com.example.commerce.payments;

public interface PaymentProvider {
    PaymentResult charge(PaymentRequest request);
}
