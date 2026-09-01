package com.store.app.order.dto;

import com.store.app.order.entity.OrderStatus;
import com.store.app.payment.entity.PaymentMethod;
import com.store.app.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One row of the admin order listing.
 */
public record AdminOrderSummaryResponse(
        Long id,
        String orderNumber,
        String customerName,
        String customerPhone,
        OrderStatus status,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        int totalItems,
        BigDecimal totalAmount,
        LocalDateTime createdAt
) {
}
