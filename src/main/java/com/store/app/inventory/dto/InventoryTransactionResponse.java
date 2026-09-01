package com.store.app.inventory.dto;

import com.store.app.inventory.entity.InventoryTransactionType;

import java.time.LocalDateTime;

/**
 * One entry of the stock movement history.
 */
public record InventoryTransactionResponse(
        Long id,
        Long productId,
        String productName,
        String sku,
        InventoryTransactionType transactionType,
        int quantity,
        int previousStock,
        int newStock,
        String reference,
        String remarks,
        LocalDateTime createdAt
) {
}
