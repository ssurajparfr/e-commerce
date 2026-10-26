package com.example.commerce.inventory;

public class OutOfStockException extends RuntimeException {
    public OutOfStockException(String message) { super(message); }
}
