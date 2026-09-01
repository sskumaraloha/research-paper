package com.store.app.order.repository;

import com.store.app.admin.dto.TopProductRow;
import com.store.app.order.entity.OrderItem;
import com.store.app.order.entity.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Best sellers aggregated from order-line snapshots (grouped by the
     * snapshot name/SKU, so rankings survive product deletion), excluding
     * orders in the given status. Limit via the Pageable.
     */
    @Query("""
            select new com.store.app.admin.dto.TopProductRow(
                oi.productName, oi.sku, sum(oi.quantity),
                sum(oi.priceAtPurchase * oi.quantity))
            from OrderItem oi
            where oi.order.status <> :excluded
            group by oi.productName, oi.sku
            order by sum(oi.quantity) desc
            """)
    List<TopProductRow> findTopSelling(@Param("excluded") OrderStatus excluded,
                                       Pageable pageable);

    /**
     * Detaches a deleted product from historical order lines; the
     * snapshot columns keep the line meaningful.
     */
    @Modifying
    @Query("update OrderItem oi set oi.product = null where oi.product.id = :productId")
    void detachProduct(@Param("productId") Long productId);
}
