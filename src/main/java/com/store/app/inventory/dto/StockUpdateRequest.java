package com.store.app.inventory.dto;

import com.store.app.inventory.entity.InventoryTransactionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Input for increasing (PURCHASE, RETURN) or decreasing (SALE, DAMAGE)
 * stock by a quantity. The service validates that the type matches the
 * direction. Mutable class because it also backs the admin form.
 */
@Getter
@Setter
@NoArgsConstructor
public class StockUpdateRequest {

    @NotNull(message = "Product is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Transaction type is required")
    private InventoryTransactionType transactionType;

    @Size(max = 100, message = "Reference must not exceed 100 characters")
    private String reference;

    @Size(max = 255, message = "Remarks must not exceed 255 characters")
    private String remarks;
}
