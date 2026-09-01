package com.store.app.product.dto;

import java.math.BigDecimal;

/**
 * Storefront browse filters. All fields are optional; {@code category}
 * is a category slug and includes the category's whole subtree.
 */
public record ProductFilterRequest(
        String search,
        String category,
        String brand,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String sort,
        int page,
        int size
) {
}
