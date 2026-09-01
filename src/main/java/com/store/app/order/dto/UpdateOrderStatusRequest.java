package com.store.app.order.dto;

import com.store.app.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Admin request to move an order to a new status. Mutable class
 * because it also backs the status form on the detail page.
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateOrderStatusRequest {

    @NotNull(message = "Please choose a status")
    private OrderStatus status;
}
