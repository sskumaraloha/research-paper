package com.store.app.inventory.repository;

import com.store.app.inventory.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    boolean existsByProductId(Long productId);

    void deleteByProductId(Long productId);

    /**
     * Locks the inventory row ({@code SELECT ... FOR UPDATE}) so concurrent
     * stock updates for the same product serialize instead of racing.
     * Must be called inside a transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.product.id = :productId")
    Optional<Inventory> findByProductIdForUpdate(@Param("productId") Long productId);

    @EntityGraph(attributePaths = "product")
    @Query("""
            select i from Inventory i join i.product p
            where (:search is null or :search = ''
                or lower(p.productName) like lower(concat('%', :search, '%'))
                or lower(p.sku) like lower(concat('%', :search, '%')))
            """)
    Page<Inventory> searchAll(@Param("search") String search, Pageable pageable);

    @EntityGraph(attributePaths = "product")
    @Query("""
            select i from Inventory i join i.product p
            where i.currentStock > 0 and i.currentStock <= i.minimumStockLevel
              and (:search is null or :search = ''
                or lower(p.productName) like lower(concat('%', :search, '%'))
                or lower(p.sku) like lower(concat('%', :search, '%')))
            """)
    Page<Inventory> searchLowStock(@Param("search") String search, Pageable pageable);

    @EntityGraph(attributePaths = "product")
    @Query("""
            select i from Inventory i join i.product p
            where i.currentStock <= 0
              and (:search is null or :search = ''
                or lower(p.productName) like lower(concat('%', :search, '%'))
                or lower(p.sku) like lower(concat('%', :search, '%')))
            """)
    Page<Inventory> searchOutOfStock(@Param("search") String search, Pageable pageable);

    @Query("select count(i) from Inventory i "
            + "where i.currentStock > 0 and i.currentStock <= i.minimumStockLevel")
    long countLowStock();

    @Query("select count(i) from Inventory i where i.currentStock <= 0")
    long countOutOfStock();
}
