package com.store.app.wishlist.repository;

import com.store.app.wishlist.entity.WishlistItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    @EntityGraph(attributePaths = "product")
    List<WishlistItem> findAllByUserIdOrderByIdDesc(Long userId);

    /** Ownership-scoped lookup: another user's item is simply not found. */
    Optional<WishlistItem> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    void deleteAllByProductId(Long productId);
}
