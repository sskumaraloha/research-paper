package com.store.app.product.repository;

import com.store.app.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlugAndActiveTrue(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    boolean existsByCategoryId(Long categoryId);

    /**
     * Paged search across product name, SKU, and brand; a null or blank
     * search term matches everything. Sorting comes from the Pageable.
     */
    @Query("""
            select p from Product p
            where (:search is null or :search = ''
                or lower(p.productName) like lower(concat('%', :search, '%'))
                or lower(p.sku) like lower(concat('%', :search, '%'))
                or lower(p.brand) like lower(concat('%', :search, '%')))
            """)
    Page<Product> search(@Param("search") String search, Pageable pageable);
}
