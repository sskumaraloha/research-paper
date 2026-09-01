package com.store.app.cart.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * The user's cart with computed totals:
 * subtotal (regular prices) − discount = total (payable).
 */
public record CartResponse(
        Long id,
        List<CartItemResponse> items,
        int totalItems,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total
) {
}
