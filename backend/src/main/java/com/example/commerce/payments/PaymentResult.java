package com.example.commerce.payments;

public record PaymentResult(String status, String providerReference, String failureCode) {
    public static PaymentResult success(String ref) { return new PaymentResult("SUCCESS", ref, null); }
}
