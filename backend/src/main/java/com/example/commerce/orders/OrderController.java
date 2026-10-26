package com.example.commerce.orders;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderRepository repository;

    public OrderController(OrderRepository repository) { this.repository = repository; }

    private String cookieValue(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if ("cart_id".equals(c.getName())) return c.getValue();
        }
        return null;
    }

    @GetMapping("/{id}")
    public Order get(@PathVariable Long id, HttpServletRequest request) {
        Order order = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found"));
        String cookie = cookieValue(request);
        if (cookie == null || !cookie.equals(order.getGuestRef())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        return order;
    }
}
