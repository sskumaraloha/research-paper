package com.store.app.product.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Customer-facing product detail. Exposes availability as a coarse
 * status instead of internal stock figures, and no cost price.
 */
public record ProductDetailResponse(
        Long id,
        String productName,
        String slug,
        String description,
        Long categoryId,
        String categoryName,
        String categorySlug,
        String brand,
        BigDecimal price,
        BigDecimal discountPrice,
        StockAvailability availability,
        List<ProductImageResponse> images
) {

    public enum StockAvailability {
        IN_STOCK,
        LOW_STOCK,
        OUT_OF_STOCK
    }
}
