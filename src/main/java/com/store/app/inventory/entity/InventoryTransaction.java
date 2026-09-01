package com.store.app.inventory.entity;

import com.store.app.common.entity.BaseEntity;
import com.store.app.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Immutable audit record of one stock movement. {@code quantity} is the
 * absolute amount moved; the direction is evident from
 * {@code previousStock} vs {@code newStock}.
 */
@Entity
@Table(
        name = "inventory_transactions",
        indexes = @Index(name = "idx_inv_txn_product", columnList = "product_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private InventoryTransactionType transactionType;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "previous_stock", nullable = false)
    private int previousStock;

    @Column(name = "new_stock", nullable = false)
    private int newStock;

    /** External reference such as a purchase-order or order number. */
    @Column(name = "reference", length = 100)
    private String reference;

    @Column(name = "remarks", length = 255)
    private String remarks;

    public InventoryTransaction(Product product, InventoryTransactionType transactionType,
                                int quantity, int previousStock, int newStock,
                                String reference, String remarks) {
        this.product = product;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.previousStock = previousStock;
        this.newStock = newStock;
        this.reference = reference;
        this.remarks = remarks;
    }
}
