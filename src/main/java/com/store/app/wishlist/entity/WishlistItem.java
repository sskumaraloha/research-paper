package com.store.app.wishlist.entity;

import com.store.app.common.entity.BaseEntity;
import com.store.app.product.entity.Product;
import com.store.app.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One product saved to a user's wishlist. The unique constraint keeps
 * each product on a user's wishlist at most once.
 */
@Entity
@Table(
        name = "wishlist_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_wishlist_user_product", columnNames = {"user_id", "product_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WishlistItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    public WishlistItem(User user, Product product) {
        this.user = user;
        this.product = product;
    }
}
