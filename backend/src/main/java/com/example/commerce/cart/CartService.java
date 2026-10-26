package com.example.commerce.cart;

import com.example.commerce.catalog.Product;
import com.example.commerce.catalog.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public Cart getOrCreate(String cookieId) {
        if (cookieId == null || cookieId.isBlank()) {
            cookieId = UUID.randomUUID().toString();
        }
        final String key = cookieId;
        return cartRepository.findByCookieId(key).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setCookieId(key);
            return cartRepository.save(cart);
        });
    }

    @Transactional
    public Cart addItem(String cookieId, Long productId, int quantity) {
        Cart cart = getOrCreate(cookieId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        if (!product.isActive()) {
            throw new IllegalArgumentException("Product not active");
        }
        CartItem existing = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst().orElse(null);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            cartItemRepository.save(existing);
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProductId(productId);
            item.setQuantity(quantity);
            cart.getItems().add(item);
            cartItemRepository.save(item);
        }
        cart.touch();
        return cartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    public Cart get(String cookieId) {
        if (cookieId == null || cookieId.isBlank()) {
            return null;
        }
        return cartRepository.findByCookieId(cookieId).orElse(null);
    }
}
