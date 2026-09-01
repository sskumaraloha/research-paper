package com.store.app.order.dto;

import com.store.app.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One row of the order history listing.
 */
public record OrderSummaryResponse(
        Long id,
        String orderNumber,
        OrderStatus status,
        int totalItems,
        BigDecimal totalAmount,
        LocalDateTime createdAt
) {
}
