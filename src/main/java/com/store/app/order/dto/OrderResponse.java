package com.store.app.order.dto;

import com.store.app.order.entity.OrderStatus;
import com.store.app.payment.entity.PaymentMethod;
import com.store.app.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full order detail.
 */
public record OrderResponse(
        Long id,
        String orderNumber,
        OrderStatus status,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        OrderAddressResponse shippingAddress,
        List<OrderItemResponse> items,
        int totalItems,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal totalAmount,
        boolean cancellable,
        LocalDateTime createdAt
) {
}
