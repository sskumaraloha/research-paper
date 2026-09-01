package com.store.app.product.dto;

import java.math.BigDecimal;

/**
 * Customer-facing product summary for grids and carousels.
 * Deliberately excludes internal figures (cost price, stock numbers).
 */
public record ProductCardResponse(
        Long id,
        String productName,
        String slug,
        String brand,
        BigDecimal price,
        BigDecimal discountPrice,
        String imageUrl,
        boolean inStock
) {
}
