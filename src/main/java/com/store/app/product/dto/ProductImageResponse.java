package com.store.app.product.dto;

/**
 * One product image in API responses.
 */
public record ProductImageResponse(
        Long id,
        String imageUrl,
        int displayOrder
) {
}
