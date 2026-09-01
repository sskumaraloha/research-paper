package com.store.app.order.dto;

import java.math.BigDecimal;

/**
 * One order line as sold (snapshot values).
 */
public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        String sku,
        BigDecimal priceAtPurchase,
        int quantity,
        BigDecimal lineTotal
) {
}
