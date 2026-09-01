package com.store.app;

import com.store.app.category.dto.CategoryRequest;
import com.store.app.category.service.CategoryService;
import com.store.app.exception.BusinessValidationException;
import com.store.app.inventory.dto.AdjustStockRequest;
import com.store.app.inventory.dto.InventoryResponse;
import com.store.app.inventory.dto.StockUpdateRequest;
import com.store.app.inventory.entity.InventoryTransactionType;
import com.store.app.inventory.service.InventoryService;
import com.store.app.product.dto.ProductRequest;
import com.store.app.product.dto.ProductResponse;
import com.store.app.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the inventory invariants: every stock change writes a
 * transaction, negative stock is impossible, and low/out-of-stock
 * detection reflects the levels.
 */
@SpringBootTest
@ActiveProfiles("test")
class InventoryServiceTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private InventoryService inventoryService;

    private Long productId;

    @BeforeEach
    void seedProduct() {
        CategoryRequest category = new CategoryRequest();
        category.setName("Inv Cat " + System.nanoTime());
        Long categoryId = categoryService.createCategory(category).id();

        ProductRequest product = new ProductRequest();
        product.setProductName("Inv Product " + System.nanoTime());
        product.setCategoryId(categoryId);
        product.setSku("INV-" + System.nanoTime());
        product.setPrice(new BigDecimal("100.00"));
        product.setStockQuantity(10);
        product.setMinimumStockLevel(3);
        product.setActive(true);
        ProductResponse created = productService.createProduct(product);
        productId = created.id();
    }

    @Test
    void productCreationInitializesInventoryWithTransaction() {
        InventoryResponse inventory = inventoryService.getByProductId(productId);
        assertThat(inventory.currentStock()).isEqualTo(10);
        assertThat(inventory.status()).isEqualTo(InventoryResponse.StockStatus.IN_STOCK);

        var history = inventoryService.getTransactions(productId, 0, 10);
        assertThat(history.content()).hasSize(1);
        assertThat(history.content().get(0).remarks()).isEqualTo("Initial stock");
        assertThat(history.content().get(0).newStock()).isEqualTo(10);
    }

    @Test
    void increaseAndDecreaseRecordTransactionsAndSyncProduct() {
        StockUpdateRequest purchase = new StockUpdateRequest();
        purchase.setProductId(productId);
        purchase.setQuantity(5);
        purchase.setTransactionType(InventoryTransactionType.PURCHASE);
        purchase.setReference("PO-1");
        assertThat(inventoryService.increaseStock(purchase).currentStock()).isEqualTo(15);

        StockUpdateRequest sale = new StockUpdateRequest();
        sale.setProductId(productId);
        sale.setQuantity(13);
        sale.setTransactionType(InventoryTransactionType.SALE);
        InventoryResponse afterSale = inventoryService.decreaseStock(sale);
        assertThat(afterSale.currentStock()).isEqualTo(2);
        assertThat(afterSale.status()).isEqualTo(InventoryResponse.StockStatus.LOW_STOCK);

        // Denormalized product copy stays in sync.
        assertThat(productService.getProductById(productId).stockQuantity()).isEqualTo(2);

        var history = inventoryService.getTransactions(productId, 0, 10);
        assertThat(history.content()).hasSize(3);
        assertThat(history.content().get(0).previousStock()).isEqualTo(15);
        assertThat(history.content().get(0).newStock()).isEqualTo(2);
    }

    @Test
    void negativeStockIsRejected() {
        StockUpdateRequest sale = new StockUpdateRequest();
        sale.setProductId(productId);
        sale.setQuantity(11);
        sale.setTransactionType(InventoryTransactionType.SALE);

        assertThatThrownBy(() -> inventoryService.decreaseStock(sale))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Insufficient stock");

        assertThat(inventoryService.getByProductId(productId).currentStock()).isEqualTo(10);
    }

    @Test
    void wrongDirectionTypeIsRejected() {
        StockUpdateRequest request = new StockUpdateRequest();
        request.setProductId(productId);
        request.setQuantity(1);
        request.setTransactionType(InventoryTransactionType.SALE);

        assertThatThrownBy(() -> inventoryService.increaseStock(request))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void adjustmentSetsAbsoluteLevelAndDetectsOutOfStock() {
        AdjustStockRequest adjust = new AdjustStockRequest();
        adjust.setProductId(productId);
        adjust.setNewStock(0);
        adjust.setRemarks("Stocktake");

        InventoryResponse adjusted = inventoryService.adjustStock(adjust);
        assertThat(adjusted.currentStock()).isZero();
        assertThat(adjusted.status()).isEqualTo(InventoryResponse.StockStatus.OUT_OF_STOCK);
        assertThat(inventoryService.countOutOfStock()).isGreaterThanOrEqualTo(1);

        var latest = inventoryService.getTransactions(productId, 0, 1).content().get(0);
        assertThat(latest.transactionType()).isEqualTo(InventoryTransactionType.ADJUSTMENT);
        assertThat(latest.quantity()).isEqualTo(10);
        assertThat(latest.previousStock()).isEqualTo(10);
        assertThat(latest.newStock()).isZero();
    }
}
