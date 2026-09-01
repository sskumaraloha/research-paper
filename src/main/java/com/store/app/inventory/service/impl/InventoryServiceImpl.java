package com.store.app.inventory.service.impl;

import com.store.app.common.dto.PageResponse;
import com.store.app.exception.BusinessValidationException;
import com.store.app.exception.ResourceNotFoundException;
import com.store.app.inventory.dto.AdjustStockRequest;
import com.store.app.inventory.dto.InventoryResponse;
import com.store.app.inventory.dto.InventoryTransactionResponse;
import com.store.app.inventory.dto.StockUpdateRequest;
import com.store.app.inventory.entity.Inventory;
import com.store.app.inventory.entity.InventoryTransaction;
import com.store.app.inventory.entity.InventoryTransactionType;
import com.store.app.inventory.mapper.InventoryMapper;
import com.store.app.inventory.repository.InventoryRepository;
import com.store.app.inventory.repository.InventoryTransactionRepository;
import com.store.app.inventory.service.InventoryService;
import com.store.app.product.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private static final Set<InventoryTransactionType> INCREASE_TYPES =
            Set.of(InventoryTransactionType.PURCHASE, InventoryTransactionType.RETURN);
    private static final Set<InventoryTransactionType> DECREASE_TYPES =
            Set.of(InventoryTransactionType.SALE, InventoryTransactionType.DAMAGE);
    private static final int MAX_PAGE_SIZE = 100;

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final InventoryMapper inventoryMapper;

    @Override
    public InventoryResponse increaseStock(StockUpdateRequest request) {
        if (!INCREASE_TYPES.contains(request.getTransactionType())) {
            throw new BusinessValidationException(
                    "Transaction type " + request.getTransactionType()
                            + " does not increase stock. Use PURCHASE or RETURN.");
        }
        Inventory inventory = changeStock(
                request.getProductId(),
                request.getTransactionType(),
                request.getQuantity(),
                request.getReference(),
                request.getRemarks());
        return inventoryMapper.toResponse(inventory);
    }

    @Override
    public InventoryResponse decreaseStock(StockUpdateRequest request) {
        if (!DECREASE_TYPES.contains(request.getTransactionType())) {
            throw new BusinessValidationException(
                    "Transaction type " + request.getTransactionType()
                            + " does not decrease stock. Use SALE or DAMAGE.");
        }
        Inventory inventory = changeStock(
                request.getProductId(),
                request.getTransactionType(),
                -request.getQuantity(),
                request.getReference(),
                request.getRemarks());
        return inventoryMapper.toResponse(inventory);
    }

    @Override
    public InventoryResponse adjustStock(AdjustStockRequest request) {
        Inventory inventory = lockedInventory(request.getProductId());
        int delta = request.getNewStock() - inventory.getCurrentStock();
        if (delta == 0) {
            throw new BusinessValidationException(
                    "Stock is already " + request.getNewStock() + "; nothing to adjust");
        }
        applyChange(inventory, InventoryTransactionType.ADJUSTMENT, delta,
                request.getReference(), request.getRemarks());
        return inventoryMapper.toResponse(inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getByProductId(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No inventory record for product id: " + productId));
        return inventoryMapper.toResponse(inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> getInventory(String filter, String search,
                                                        int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("product.productName").ascending());
        String term = StringUtils.hasText(search) ? search.trim() : null;

        Page<Inventory> result = switch (filter == null ? "all" : filter) {
            case "low" -> inventoryRepository.searchLowStock(term, pageable);
            case "out" -> inventoryRepository.searchOutOfStock(term, pageable);
            default -> inventoryRepository.searchAll(term, pageable);
        };
        return PageResponse.from(result.map(inventoryMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryTransactionResponse> getTransactions(Long productId,
                                                                      int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE));

        Page<InventoryTransaction> result = productId == null
                ? transactionRepository.findAllByOrderByIdDesc(pageable)
                : transactionRepository.findAllByProductIdOrderByIdDesc(productId, pageable);
        return PageResponse.from(result.map(inventoryMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public long countLowStock() {
        return inventoryRepository.countLowStock();
    }

    @Override
    @Transactional(readOnly = true)
    public long countOutOfStock() {
        return inventoryRepository.countOutOfStock();
    }

    // ------------------------------------------------------------------
    // Internal API for the product module
    // ------------------------------------------------------------------

    @Override
    public void initializeInventory(Product product) {
        if (inventoryRepository.existsByProductId(product.getId())) {
            return;
        }
        Inventory inventory = new Inventory(
                product, product.getStockQuantity(), product.getMinimumStockLevel());
        inventoryRepository.save(inventory);

        if (product.getStockQuantity() > 0) {
            transactionRepository.save(new InventoryTransaction(
                    product, InventoryTransactionType.ADJUSTMENT,
                    product.getStockQuantity(), 0, product.getStockQuantity(),
                    null, "Initial stock"));
        }
    }

    @Override
    public void syncFromProductEdit(Product product, int newStock, int newMinimumStockLevel) {
        Inventory inventory = inventoryRepository.findByProductIdForUpdate(product.getId())
                .orElseGet(() -> inventoryRepository.save(
                        new Inventory(product, product.getStockQuantity(),
                                product.getMinimumStockLevel())));

        inventory.setMinimumStockLevel(newMinimumStockLevel);
        product.setMinimumStockLevel(newMinimumStockLevel);

        int delta = newStock - inventory.getCurrentStock();
        if (delta != 0) {
            applyChange(inventory, InventoryTransactionType.ADJUSTMENT, delta,
                    null, "Adjusted via product edit");
        } else {
            inventoryRepository.save(inventory);
        }
    }

    @Override
    public void deleteInventoryForProduct(Long productId) {
        transactionRepository.deleteAllByProductId(productId);
        inventoryRepository.findByProductId(productId)
                .ifPresent(inventoryRepository::delete);
    }

    // ------------------------------------------------------------------

    /**
     * The single write path for stock: locks the row, validates the new
     * level, updates inventory + the product's denormalized copy, and
     * records the transaction — all atomically.
     */
    private Inventory changeStock(Long productId, InventoryTransactionType type,
                                  int delta, String reference, String remarks) {
        Inventory inventory = lockedInventory(productId);
        applyChange(inventory, type, delta, reference, remarks);
        return inventory;
    }

    private void applyChange(Inventory inventory, InventoryTransactionType type,
                             int delta, String reference, String remarks) {
        int previousStock = inventory.getCurrentStock();
        int newStock = previousStock + delta;
        if (newStock < 0) {
            throw new BusinessValidationException(
                    "Insufficient stock for \"" + inventory.getProduct().getProductName()
                            + "\": available " + previousStock + ", requested " + (-delta));
        }

        inventory.setCurrentStock(newStock);
        inventory.getProduct().setStockQuantity(newStock);
        inventoryRepository.save(inventory);

        transactionRepository.save(new InventoryTransaction(
                inventory.getProduct(), type, Math.abs(delta),
                previousStock, newStock,
                normalize(reference), normalize(remarks)));
    }

    private Inventory lockedInventory(Long productId) {
        return inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No inventory record for product id: " + productId));
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
