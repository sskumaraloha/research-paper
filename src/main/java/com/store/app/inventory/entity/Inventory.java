package com.store.app.inventory.entity;

import com.store.app.common.entity.BaseEntity;
import com.store.app.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The authoritative stock record for one product. Every change to
 * {@code currentStock} MUST go through the inventory service, which
 * locks this row pessimistically and writes an
 * {@link InventoryTransaction} in the same database transaction.
 */
@Entity
@Table(
        name = "inventories",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_inventories_product", columnNames = "product_id")
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "current_stock", nullable = false)
    private int currentStock;

    @Column(name = "minimum_stock_level", nullable = false)
    private int minimumStockLevel;

    public Inventory(Product product, int currentStock, int minimumStockLevel) {
        this.product = product;
        this.currentStock = currentStock;
        this.minimumStockLevel = minimumStockLevel;
    }

    public boolean isOutOfStock() {
        return currentStock <= 0;
    }

    public boolean isLowStock() {
        return currentStock > 0 && currentStock <= minimumStockLevel;
    }
}
