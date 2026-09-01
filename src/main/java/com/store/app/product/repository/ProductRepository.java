package com.store.app.product.repository;

import com.store.app.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
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

    /**
     * Storefront browse: active products with optional search, category
     * subtree, brand, and effective-price (discount if set) range filters.
     * When {@code filterCategory} is false the category list is ignored.
     */
    @Query("""
            select p from Product p
            where p.active = true
              and (:search is null or :search = ''
                  or lower(p.productName) like lower(concat('%', :search, '%'))
                  or lower(p.brand) like lower(concat('%', :search, '%')))
              and (:filterCategory = false or p.category.id in :categoryIds)
              and (:brand is null or :brand = '' or p.brand = :brand)
              and (:minPrice is null or coalesce(p.discountPrice, p.price) >= :minPrice)
              and (:maxPrice is null or coalesce(p.discountPrice, p.price) <= :maxPrice)
            """)
    Page<Product> browse(@Param("search") String search,
                         @Param("filterCategory") boolean filterCategory,
                         @Param("categoryIds") List<Long> categoryIds,
                         @Param("brand") String brand,
                         @Param("minPrice") BigDecimal minPrice,
                         @Param("maxPrice") BigDecimal maxPrice,
                         Pageable pageable);

    List<Product> findTop8ByActiveTrueOrderByCreatedAtDescIdDesc();

    List<Product> findTop8ByActiveTrueAndDiscountPriceIsNotNullOrderByCreatedAtDescIdDesc();

    List<Product> findTop8ByActiveTrueOrderByStockQuantityDescIdDesc();

    List<Product> findTop4ByActiveTrueAndCategoryIdAndIdNotOrderByCreatedAtDescIdDesc(
            Long categoryId, Long excludeId);

    @Query("""
            select distinct p.brand from Product p
            where p.active = true and p.brand is not null
            order by p.brand
            """)
    List<String> findActiveBrands();
}
