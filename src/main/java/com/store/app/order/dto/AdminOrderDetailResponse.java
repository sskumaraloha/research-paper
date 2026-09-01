package com.store.app.order.dto;

import com.store.app.order.entity.OrderStatus;

import java.util.Set;

/**
 * Full admin view of an order: the order itself, the customer behind
 * it, and which statuses it may legally move to next.
 */
public record AdminOrderDetailResponse(
        OrderResponse order,
        String customerName,
        String customerPhone,
        String customerEmail,
        Set<OrderStatus> allowedNextStatuses
) {
}
