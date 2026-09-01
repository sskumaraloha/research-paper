package com.store.app.inventory.repository;

import com.store.app.inventory.entity.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryTransactionRepository
        extends JpaRepository<InventoryTransaction, Long> {

    @EntityGraph(attributePaths = "product")
    Page<InventoryTransaction> findAllByOrderByIdDesc(Pageable pageable);

    @EntityGraph(attributePaths = "product")
    Page<InventoryTransaction> findAllByProductIdOrderByIdDesc(Long productId, Pageable pageable);

    void deleteAllByProductId(Long productId);
}
