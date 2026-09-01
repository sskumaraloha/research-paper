package com.store.app.order.entity;

import com.store.app.common.entity.BaseEntity;
import com.store.app.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One order line, storing a snapshot of the product as sold —
 * {@code productName}, {@code sku}, and {@code priceAtPurchase} are
 * copied at checkout and never re-read from the catalog. The product
 * reference itself is optional so history survives product deletion.
 */
@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** Link back to the catalog; nullable so order history outlives products. */
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(name = "sku", nullable = false, length = 50)
    private String sku;

    /** Effective selling price at checkout time. */
    @Column(name = "price_at_purchase", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtPurchase;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    public OrderItem(Product product, String productName, String sku,
                     BigDecimal priceAtPurchase, int quantity) {
        this.product = product;
        this.productName = productName;
        this.sku = sku;
        this.priceAtPurchase = priceAtPurchase;
        this.quantity = quantity;
    }

    public BigDecimal getLineTotal() {
        return priceAtPurchase.multiply(BigDecimal.valueOf(quantity));
    }
}
