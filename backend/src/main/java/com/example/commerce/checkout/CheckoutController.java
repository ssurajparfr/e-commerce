package com.example.commerce.checkout;

import com.example.commerce.config.TraceFilter;
import com.example.commerce.orders.Order;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {
    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) { this.checkoutService = checkoutService; }

    @PostMapping
    public Order checkout(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                          @RequestBody(required = false) Map<String, Object> body,
                          HttpServletRequest request) {
        String cookie = null;
        if (request.getCookies() != null) {
            for (var c : request.getCookies()) {
                if ("cart_id".equals(c.getName())) cookie = c.getValue();
            }
        }
        String traceId = (String) request.getAttribute(TraceFilter.TRACE_HEADER);
        String correlationId = (String) request.getAttribute(TraceFilter.CORRELATION_HEADER);
        return checkoutService.checkout(cookie, idempotencyKey, correlationId, traceId);
    }
}
