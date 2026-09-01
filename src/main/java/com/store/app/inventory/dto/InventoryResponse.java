package com.store.app.inventory.dto;

import java.time.LocalDateTime;

/**
 * One product's current stock position.
 */
public record InventoryResponse(
        Long id,
        Long productId,
        String productName,
        String sku,
        int currentStock,
        int minimumStockLevel,
        StockStatus status,
        LocalDateTime updatedAt
) {

    public enum StockStatus {
        IN_STOCK,
        LOW_STOCK,
        OUT_OF_STOCK
    }
}
