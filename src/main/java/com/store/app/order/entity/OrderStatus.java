package com.store.app.order.entity;

/**
 * Order lifecycle. Customers may cancel only while PENDING or CONFIRMED.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    PACKED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
