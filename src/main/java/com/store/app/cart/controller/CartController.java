package com.store.app.cart.controller;

import com.store.app.cart.dto.AddToCartRequest;
import com.store.app.cart.dto.CartResponse;
import com.store.app.cart.dto.UpdateCartItemRequest;
import com.store.app.cart.service.CartService;
import com.store.app.security.StoreUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for the authenticated user's shopping cart (JWT).
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal StoreUserDetails principal) {
        return ResponseEntity.ok(cartService.getCart(userId(principal)));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addToCart(
            @AuthenticationPrincipal StoreUserDetails principal,
            @Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(cartService.addToCart(userId(principal), request));
    }

    @PatchMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateQuantity(
            @AuthenticationPrincipal StoreUserDetails principal,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(cartService.updateItemQuantity(
                userId(principal), itemId, request.quantity()));
    }

    @PostMapping("/items/{itemId}/increase")
    public ResponseEntity<CartResponse> increaseQuantity(
            @AuthenticationPrincipal StoreUserDetails principal,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.increaseItemQuantity(userId(principal), itemId));
    }

    @PostMapping("/items/{itemId}/decrease")
    public ResponseEntity<CartResponse> decreaseQuantity(
            @AuthenticationPrincipal StoreUserDetails principal,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.decreaseItemQuantity(userId(principal), itemId));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> removeItem(
            @AuthenticationPrincipal StoreUserDetails principal,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeItem(userId(principal), itemId));
    }

    @DeleteMapping
    public ResponseEntity<CartResponse> clearCart(
            @AuthenticationPrincipal StoreUserDetails principal) {
        return ResponseEntity.ok(cartService.clearCart(userId(principal)));
    }

    private Long userId(StoreUserDetails principal) {
        return principal.getUser().getId();
    }
}
