package com.store.app.payment.service;

import com.store.app.order.entity.Order;
import com.store.app.order.entity.OrderStatus;
import com.store.app.payment.entity.Payment;
import com.store.app.payment.entity.PaymentMethod;

/**
 * Strategy for one payment method. Each implementation handles exactly
 * one {@link PaymentMethod}; the {@link PaymentServiceRegistry} routes
 * calls to the right implementation.
 * <p>
 * Adding a gateway (e.g. Razorpay) means adding one bean implementing
 * this interface — checkout, cancellation, and the registry pick it up
 * automatically with no changes to the order module.
 */
public interface PaymentService {

    /** The single payment method this implementation handles. */
    PaymentMethod getSupportedMethod();

    /**
     * The state a freshly placed order starts in for this method:
     * methods with no upfront payment (COD) confirm immediately, while
     * gateway methods keep the order PENDING until payment completes.
     */
    OrderStatus initialOrderStatus();

    /**
     * Creates the (unsaved) payment record for a new order; persisted
     * by the order cascade inside the checkout transaction. Gateway
     * implementations also initiate the external payment here.
     */
    Payment initiatePayment(Order order);

    /**
     * Marks the payment as collected — on delivery for COD, or when a
     * gateway callback/webhook confirms an online payment.
     *
     * @param transactionReference external reference; may be null for COD
     */
    Payment confirmPayment(Payment payment, String transactionReference);

    /**
     * Settles the payment when its order is cancelled: an already PAID
     * payment is refunded, an uncollected one is marked FAILED.
     */
    Payment cancelPayment(Payment payment);
}
