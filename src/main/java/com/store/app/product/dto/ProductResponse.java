package com.store.app.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full product representation for admin APIs and pages.
 */
public record ProductResponse(
        Long id,
        String productName,
        String slug,
        String description,
        Long categoryId,
        String categoryName,
        String brand,
        String sku,
        BigDecimal price,
        BigDecimal discountPrice,
        BigDecimal costPrice,
        int stockQuantity,
        int minimumStockLevel,
        boolean active,
        List<ProductImageResponse> images,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
