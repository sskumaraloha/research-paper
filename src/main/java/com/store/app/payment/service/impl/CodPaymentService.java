package com.store.app.payment.service.impl;

import com.store.app.order.entity.Order;
import com.store.app.order.entity.OrderStatus;
import com.store.app.payment.entity.Payment;
import com.store.app.payment.entity.PaymentMethod;
import com.store.app.payment.entity.PaymentStatus;
import com.store.app.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Cash on delivery: no upfront payment, so orders confirm immediately
 * and the payment stays PENDING until collected at the door.
 */
@Slf4j
@Service
public class CodPaymentService implements PaymentService {

    @Override
    public PaymentMethod getSupportedMethod() {
        return PaymentMethod.CASH_ON_DELIVERY;
    }

    @Override
    public OrderStatus initialOrderStatus() {
        return OrderStatus.CONFIRMED;
    }

    @Override
    public Payment initiatePayment(Order order) {
        return new Payment(
                PaymentMethod.CASH_ON_DELIVERY,
                PaymentStatus.PENDING,
                order.getTotalAmount());
    }

    @Override
    public Payment confirmPayment(Payment payment, String transactionReference) {
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setTransactionReference(
                transactionReference != null ? transactionReference : "COD-COLLECTED");
        log.info("COD payment collected for order {}", payment.getOrder().getOrderNumber());
        return payment;
    }

    @Override
    public Payment cancelPayment(Payment payment) {
        payment.setStatus(payment.getStatus() == PaymentStatus.PAID
                ? PaymentStatus.REFUNDED
                : PaymentStatus.FAILED);
        return payment;
    }
}
