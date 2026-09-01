package com.store.app.wishlist.dto;

import com.store.app.product.dto.ProductCardResponse;

import java.time.LocalDateTime;

/**
 * One wishlist entry with its product summary.
 */
public record WishlistItemResponse(
        Long id,
        ProductCardResponse product,
        LocalDateTime addedAt
) {
}
