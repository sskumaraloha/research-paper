package com.store.app.wishlist.controller;

import com.store.app.security.StoreUserDetails;
import com.store.app.wishlist.dto.WishlistItemResponse;
import com.store.app.wishlist.service.WishlistService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API for the authenticated customer's wishlist
 * (JWT + ROLE_CUSTOMER via /api/customer/**).
 */
@RestController
@RequestMapping("/api/customer/wishlist")
@RequiredArgsConstructor
@Validated
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<List<WishlistItemResponse>> getWishlist(
            @AuthenticationPrincipal StoreUserDetails principal) {
        return ResponseEntity.ok(wishlistService.getWishlist(userId(principal)));
    }

    @PostMapping("/items")
    public ResponseEntity<WishlistItemResponse> addProduct(
            @AuthenticationPrincipal StoreUserDetails principal,
            @RequestParam("productId") @NotNull Long productId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(wishlistService.addProduct(userId(principal), productId));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItem(
            @AuthenticationPrincipal StoreUserDetails principal,
            @PathVariable Long itemId) {
        wishlistService.removeItem(userId(principal), itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/items/{itemId}/move-to-cart")
    public ResponseEntity<Void> moveToCart(
            @AuthenticationPrincipal StoreUserDetails principal,
            @PathVariable Long itemId) {
        wishlistService.moveToCart(userId(principal), itemId);
        return ResponseEntity.noContent().build();
    }

    private Long userId(StoreUserDetails principal) {
        return principal.getUser().getId();
    }
}
