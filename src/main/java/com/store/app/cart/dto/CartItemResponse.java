package com.store.app.cart.dto;

import java.math.BigDecimal;

/**
 * One cart line. {@code unitPrice} is the product's regular price,
 * {@code effectiveUnitPrice} the current selling price (discounted if
 * applicable) used for totals; {@code priceChanged} flags items whose
 * selling price differs from when they were added.
 */
public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        String slug,
        String imageUrl,
        BigDecimal unitPrice,
        BigDecimal effectiveUnitPrice,
        BigDecimal priceAtAddition,
        boolean priceChanged,
        int quantity,
        int availableStock,
        BigDecimal lineTotal
) {
}
