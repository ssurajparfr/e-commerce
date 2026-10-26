package com.example.commerce.cart;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) { this.cartService = cartService; }

    private String cookieValue(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if ("cart_id".equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private void setCookie(HttpServletResponse response, String value) {
        Cookie cookie = new Cookie("cart_id", value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }

    @GetMapping
    public Cart getCart(HttpServletRequest request, HttpServletResponse response) {
        String cookie = cookieValue(request);
        Cart cart = cartService.get(cookie);
        if (cart == null) {
            cart = cartService.getOrCreate(cookie);
            setCookie(response, cart.getCookieId());
        }
        return cart;
    }

    @PostMapping("/items")
    public Cart addItem(@RequestBody Map<String, Object> body, HttpServletRequest request, HttpServletResponse response) {
        String cookie = cookieValue(request);
        Cart cart = cartService.getOrCreate(cookie);
        setCookie(response, cart.getCookieId());
        Long productId = Long.valueOf(body.get("productId").toString());
        int quantity = Integer.parseInt(body.getOrDefault("quantity", 1).toString());
        return cartService.addItem(cart.getCookieId(), productId, quantity);
    }
}
