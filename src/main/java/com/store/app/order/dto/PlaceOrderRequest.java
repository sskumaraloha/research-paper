package com.store.app.order.dto;

import com.store.app.payment.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Checkout submission: which saved address to ship to and how to pay.
 * Mutable class because it also backs the checkout Thymeleaf form.
 */
@Getter
@Setter
@NoArgsConstructor
public class PlaceOrderRequest {

    @NotNull(message = "Please select a delivery address")
    private Long addressId;

    @NotNull(message = "Please select a payment method")
    private PaymentMethod paymentMethod;
}
