package com.store.app.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Input for setting a cart item's quantity to an absolute value.
 */
public record UpdateCartItemRequest(

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 99, message = "Quantity must not exceed 99")
        Integer quantity
) {
}
