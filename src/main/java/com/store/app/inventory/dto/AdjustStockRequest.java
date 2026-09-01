package com.store.app.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Input for setting a product's stock to an absolute level
 * (ADJUSTMENT transaction, e.g. after a physical stock count).
 * Mutable class because it also backs the admin form.
 */
@Getter
@Setter
@NoArgsConstructor
public class AdjustStockRequest {

    @NotNull(message = "Product is required")
    private Long productId;

    @NotNull(message = "New stock level is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer newStock;

    @Size(max = 100, message = "Reference must not exceed 100 characters")
    private String reference;

    @Size(max = 255, message = "Remarks must not exceed 255 characters")
    private String remarks;
}
