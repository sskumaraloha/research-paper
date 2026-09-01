package com.store.app.payment.entity;

/**
 * How the customer pays. ONLINE is accepted at checkout and processed
 * by the payment module (gateway integration phase); CASH_ON_DELIVERY
 * is collected on delivery.
 */
public enum PaymentMethod {
    CASH_ON_DELIVERY,
    ONLINE
}
