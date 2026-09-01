package com.store.app.inventory.controller;

import com.store.app.common.dto.PageResponse;
import com.store.app.inventory.dto.AdjustStockRequest;
import com.store.app.inventory.dto.InventoryResponse;
import com.store.app.inventory.dto.InventoryTransactionResponse;
import com.store.app.inventory.dto.StockUpdateRequest;
import com.store.app.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin REST API for inventory. Guarded by ROLE_ADMIN through the
 * /api/admin/** security rule (JWT).
 */
@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
public class AdminInventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<PageResponse<InventoryResponse>> getInventory(
            @RequestParam(name = "filter", defaultValue = "all") String filter,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(inventoryService.getInventory(filter, search, page, size));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<PageResponse<InventoryResponse>> getLowStock(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(inventoryService.getInventory("low", search, page, size));
    }

    @GetMapping("/out-of-stock")
    public ResponseEntity<PageResponse<InventoryResponse>> getOutOfStock(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(inventoryService.getInventory("out", search, page, size));
    }

    @GetMapping("/transactions")
    public ResponseEntity<PageResponse<InventoryTransactionResponse>> getTransactions(
            @RequestParam(name = "productId", required = false) Long productId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(inventoryService.getTransactions(productId, page, size));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<InventoryResponse> getForProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getByProductId(productId));
    }

    @PostMapping("/increase")
    public ResponseEntity<InventoryResponse> increaseStock(
            @Valid @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(inventoryService.increaseStock(request));
    }

    @PostMapping("/decrease")
    public ResponseEntity<InventoryResponse> decreaseStock(
            @Valid @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(inventoryService.decreaseStock(request));
    }

    @PostMapping("/adjust")
    public ResponseEntity<InventoryResponse> adjustStock(
            @Valid @RequestBody AdjustStockRequest request) {
        return ResponseEntity.ok(inventoryService.adjustStock(request));
    }
}
