package com.store.app.product.service;

import com.store.app.common.dto.PageResponse;
import com.store.app.product.dto.ProductCardResponse;
import com.store.app.product.dto.ProductDetailResponse;
import com.store.app.product.dto.ProductFilterRequest;
import com.store.app.product.dto.ProductRequest;
import com.store.app.product.dto.ProductResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Product catalog management.
 */
public interface ProductService {

    /**
     * @throws com.store.app.exception.DuplicateResourceException
     *         if the SKU is already in use
     * @throws com.store.app.exception.ResourceNotFoundException
     *         if the category does not exist
     * @throws com.store.app.exception.BusinessValidationException
     *         if the discount price is not lower than the price
     */
    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Long id, ProductRequest request);

    /** Deletes the product and its stored image files. */
    void deleteProduct(Long id);

    ProductResponse setProductActive(Long id, boolean active);

    ProductResponse getProductById(Long id);

    /**
     * Paged product listing with optional search (product name, SKU, brand)
     * and sorting.
     *
     * @param sort one of {@code newest}, {@code price_asc}, {@code price_desc};
     *             anything else falls back to {@code newest}
     */
    PageResponse<ProductResponse> searchProducts(String search, String sort, int page, int size);

    /**
     * Stores an uploaded image and attaches it to the product (appended
     * after the current last image).
     */
    ProductResponse addProductImage(Long productId, MultipartFile file);

    /** Removes an image from the product and deletes the stored file. */
    ProductResponse deleteProductImage(Long productId, Long imageId);

    // ------------------------------------------------------------------
    // Storefront (customer-facing, active products only)
    // ------------------------------------------------------------------

    /** Paged storefront browsing with search/category/brand/price filters. */
    PageResponse<ProductCardResponse> browseProducts(ProductFilterRequest filter);

    /**
     * Public product detail by slug.
     *
     * @throws com.store.app.exception.ResourceNotFoundException
     *         if no active product has this slug
     */
    ProductDetailResponse getProductDetail(String slug);

    /** Active products from the same category, excluding the product itself. */
    List<ProductCardResponse> getRelatedProducts(Long categoryId, Long excludeProductId);

    /** Products currently on offer (discount price set). */
    List<ProductCardResponse> getFeaturedProducts();

    /** Most recently added active products. */
    List<ProductCardResponse> getNewProducts();

    /**
     * "Popular" products. Until order data exists this is a placeholder
     * heuristic (best-stocked staples); it will switch to sales-based
     * ranking once the order module lands.
     */
    List<ProductCardResponse> getPopularProducts();

    /** Distinct brands of active products, for the storefront filter. */
    List<String> getActiveBrands();
}
