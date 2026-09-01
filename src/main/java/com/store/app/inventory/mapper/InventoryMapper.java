package com.store.app.inventory.mapper;

import com.store.app.inventory.dto.InventoryResponse;
import com.store.app.inventory.dto.InventoryTransactionResponse;
import com.store.app.inventory.entity.Inventory;
import com.store.app.inventory.entity.InventoryTransaction;
import org.springframework.stereotype.Component;

/**
 * Maps inventory entities to DTOs.
 */
@Component
public class InventoryMapper {

    public InventoryResponse toResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getProduct().getId(),
                inventory.getProduct().getProductName(),
                inventory.getProduct().getSku(),
                inventory.getCurrentStock(),
                inventory.getMinimumStockLevel(),
                statusOf(inventory),
                inventory.getUpdatedAt()
        );
    }

    public InventoryTransactionResponse toResponse(InventoryTransaction transaction) {
        return new InventoryTransactionResponse(
                transaction.getId(),
                transaction.getProduct().getId(),
                transaction.getProduct().getProductName(),
                transaction.getProduct().getSku(),
                transaction.getTransactionType(),
                transaction.getQuantity(),
                transaction.getPreviousStock(),
                transaction.getNewStock(),
                transaction.getReference(),
                transaction.getRemarks(),
                transaction.getCreatedAt()
        );
    }

    private InventoryResponse.StockStatus statusOf(Inventory inventory) {
        if (inventory.isOutOfStock()) {
            return InventoryResponse.StockStatus.OUT_OF_STOCK;
        }
        if (inventory.isLowStock()) {
            return InventoryResponse.StockStatus.LOW_STOCK;
        }
        return InventoryResponse.StockStatus.IN_STOCK;
    }
}
