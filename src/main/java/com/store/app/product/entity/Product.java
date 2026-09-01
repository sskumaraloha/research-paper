package com.store.app.product.entity;

import com.store.app.category.entity.Category;
import com.store.app.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A sellable product. Prices are BigDecimal; {@code discountPrice} is the
 * optional selling price when the product is on offer, {@code costPrice}
 * the purchase cost (admin-only figure for margins).
 */
@Entity
@Table(
        name = "products",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_products_slug", columnNames = "slug"),
                @UniqueConstraint(name = "uk_products_sku", columnNames = "sku")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(name = "slug", nullable = false, length = 170)
    private String slug;

    @Column(name = "description", length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "brand", length = 100)
    private String brand;

    /** Stock-keeping unit, unique product code. */
    @Column(name = "sku", nullable = false, length = 50)
    private String sku;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "discount_price", precision = 10, scale = 2)
    private BigDecimal discountPrice;

    @Column(name = "cost_price", precision = 10, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    /** Threshold below which the product counts as low stock. */
    @Column(name = "minimum_stock_level", nullable = false)
    private int minimumStockLevel;

    @Column(name = "active", nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<ProductImage> images = new ArrayList<>();

    public Product(String productName, String slug, String description, Category category,
                   String brand, String sku, BigDecimal price, BigDecimal discountPrice,
                   BigDecimal costPrice, int stockQuantity, int minimumStockLevel,
                   boolean active) {
        this.productName = productName;
        this.slug = slug;
        this.description = description;
        this.category = category;
        this.brand = brand;
        this.sku = sku;
        this.price = price;
        this.discountPrice = discountPrice;
        this.costPrice = costPrice;
        this.stockQuantity = stockQuantity;
        this.minimumStockLevel = minimumStockLevel;
        this.active = active;
    }

    public void addImage(ProductImage image) {
        images.add(image);
        image.setProduct(this);
    }

    public void removeImage(ProductImage image) {
        images.remove(image);
        image.setProduct(null);
    }
}
