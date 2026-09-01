package com.store.app.inventory.service;

import com.store.app.common.dto.PageResponse;
import com.store.app.inventory.dto.AdjustStockRequest;
import com.store.app.inventory.dto.InventoryResponse;
import com.store.app.inventory.dto.InventoryTransactionResponse;
import com.store.app.inventory.dto.StockUpdateRequest;
import com.store.app.product.entity.Product;

/**
 * Inventory management. Every stock change locks the inventory row
 * pessimistically and writes an InventoryTransaction in the same
 * database transaction — there is no code path that changes stock
 * without an audit record.
 */
public interface InventoryService {

    /**
     * Increases stock (PURCHASE or RETURN).
     *
     * @throws com.store.app.exception.BusinessValidationException
     *         if the transaction type does not increase stock
     */
    InventoryResponse increaseStock(StockUpdateRequest request);

    /**
     * Decreases stock (SALE or DAMAGE). Negative stock is rejected.
     *
     * @throws com.store.app.exception.BusinessValidationException
     *         if the type does not decrease stock, or stock would go negative
     */
    InventoryResponse decreaseStock(StockUpdateRequest request);

    /** Sets stock to an absolute level via an ADJUSTMENT transaction. */
    InventoryResponse adjustStock(AdjustStockRequest request);

    InventoryResponse getByProductId(Long productId);

    /**
     * Paged inventory listing.
     *
     * @param filter {@code all}, {@code low} (0 &lt; stock &le; minimum),
     *               or {@code out} (stock &le; 0)
     */
    PageResponse<InventoryResponse> getInventory(String filter, String search,
                                                 int page, int size);

    /** Movement history, newest first; {@code productId} null for all products. */
    PageResponse<InventoryTransactionResponse> getTransactions(Long productId,
                                                               int page, int size);

    long countLowStock();

    long countOutOfStock();

    // ------------------------------------------------------------------
    // Internal API used by the product module (same-transaction calls)
    // ------------------------------------------------------------------

    /** Creates the inventory row for a new product (+ initial-stock record). */
    void initializeInventory(Product product);

    /**
     * Applies a product-form edit: syncs the minimum stock level and, when
     * the stock value changed, records an ADJUSTMENT transaction.
     */
    void syncFromProductEdit(Product product, int newStock, int newMinimumStockLevel);

    /** Removes the inventory row and movement history of a deleted product. */
    void deleteInventoryForProduct(Long productId);
}
