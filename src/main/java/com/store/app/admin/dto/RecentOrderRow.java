package com.store.app.admin.dto;

import com.store.app.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One row of the dashboard's recent-orders table.
 */
public record RecentOrderRow(
        Long id,
        String orderNumber,
        String customerName,
        OrderStatus status,
        BigDecimal totalAmount,
        LocalDateTime createdAt
) {
}
