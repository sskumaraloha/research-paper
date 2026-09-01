package com.store.app.product.service.impl;

import com.store.app.cart.service.CartService;
import com.store.app.category.entity.Category;
import com.store.app.category.repository.CategoryRepository;
import com.store.app.common.dto.PageResponse;
import com.store.app.common.storage.FileStorageService;
import com.store.app.exception.BusinessValidationException;
import com.store.app.exception.DuplicateResourceException;
import com.store.app.exception.ResourceNotFoundException;
import com.store.app.inventory.service.InventoryService;
import com.store.app.product.dto.ProductCardResponse;
import com.store.app.product.dto.ProductDetailResponse;
import com.store.app.product.dto.ProductFilterRequest;
import com.store.app.product.dto.ProductRequest;
import com.store.app.product.dto.ProductResponse;
import com.store.app.product.entity.Product;
import com.store.app.product.entity.ProductImage;
import com.store.app.product.mapper.ProductMapper;
import com.store.app.product.repository.ProductRepository;
import com.store.app.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private static final String IMAGE_SUBDIRECTORY = "products";
    private static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final FileStorageService fileStorageService;
    private final InventoryService inventoryService;
    private final CartService cartService;

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        String sku = normalizeSku(request.getSku());
        if (productRepository.existsBySku(sku)) {
            throw new DuplicateResourceException("sku", "SKU is already in use: " + sku);
        }
        validatePricing(request);

        Category category = findCategory(request.getCategoryId());
        Product product = new Product(
                request.getProductName().trim(),
                generateUniqueSlug(request.getProductName(), null),
                normalize(request.getDescription()),
                category,
                normalize(request.getBrand()),
                sku,
                request.getPrice(),
                request.getDiscountPrice(),
                request.getCostPrice(),
                request.getStockQuantity(),
                request.getMinimumStockLevel(),
                request.isActive()
        );
        Product saved = productRepository.save(product);
        inventoryService.initializeInventory(saved);
        return productMapper.toResponse(saved);
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = findProduct(id);

        String sku = normalizeSku(request.getSku());
        if (!product.getSku().equals(sku) && productRepository.existsBySkuAndIdNot(sku, id)) {
            throw new DuplicateResourceException("sku", "SKU is already in use: " + sku);
        }
        validatePricing(request);

        String newName = request.getProductName().trim();
        if (!product.getProductName().equals(newName)) {
            product.setSlug(generateUniqueSlug(newName, id));
        }

        product.setProductName(newName);
        product.setDescription(normalize(request.getDescription()));
        product.setCategory(findCategory(request.getCategoryId()));
        product.setBrand(normalize(request.getBrand()));
        product.setSku(sku);
        product.setPrice(request.getPrice());
        product.setDiscountPrice(request.getDiscountPrice());
        product.setCostPrice(request.getCostPrice());
        product.setActive(request.isActive());

        Product saved = productRepository.save(product);
        // Stock and minimum level are owned by the inventory module: a stock
        // change from the product form becomes an ADJUSTMENT transaction.
        inventoryService.syncFromProductEdit(
                saved, request.getStockQuantity(), request.getMinimumStockLevel());
        return productMapper.toResponse(saved);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = findProduct(id);
        List<String> imageUrls = product.getImages().stream()
                .map(ProductImage::getImageUrl)
                .toList();

        cartService.removeProductFromCarts(id);
        inventoryService.deleteInventoryForProduct(id);
        productRepository.delete(product);
        imageUrls.forEach(fileStorageService::delete);
    }

    @Override
    public ProductResponse setProductActive(Long id, boolean active) {
        Product product = findProduct(id);
        product.setActive(active);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return productMapper.toResponse(findProduct(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(String search, String sort,
                                                        int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                resolveSort(sort));

        String term = StringUtils.hasText(search) ? search.trim() : null;
        Page<ProductResponse> result = productRepository.search(term, pageable)
                .map(productMapper::toResponse);
        return PageResponse.from(result);
    }

    @Override
    public ProductResponse addProductImage(Long productId, MultipartFile file) {
        Product product = findProduct(productId);

        String imageUrl = fileStorageService.storeImage(file, IMAGE_SUBDIRECTORY);
        int nextOrder = product.getImages().stream()
                .mapToInt(ProductImage::getDisplayOrder)
                .max()
                .orElse(0) + 1;

        product.addImage(new ProductImage(imageUrl, nextOrder));
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    public ProductResponse deleteProductImage(Long productId, Long imageId) {
        Product product = findProduct(productId);
        ProductImage image = product.getImages().stream()
                .filter(i -> i.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Image not found with id " + imageId + " on product " + productId));

        String imageUrl = image.getImageUrl();
        product.removeImage(image);
        Product saved = productRepository.save(product);
        fileStorageService.delete(imageUrl);
        return productMapper.toResponse(saved);
    }

    // ------------------------------------------------------------------
    // Storefront
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductCardResponse> browseProducts(ProductFilterRequest filter) {
        Pageable pageable = PageRequest.of(
                Math.max(filter.page(), 0),
                Math.min(Math.max(filter.size(), 1), MAX_PAGE_SIZE),
                resolveSort(filter.sort()));

        boolean filterCategory = false;
        List<Long> categoryIds = List.of(-1L);
        if (StringUtils.hasText(filter.category())) {
            filterCategory = true;
            categoryIds = categoryRepository.findBySlugAndActiveTrue(filter.category())
                    .map(category -> List.copyOf(collectSubtreeIds(category.getId())))
                    // Unknown category slug matches nothing rather than everything.
                    .orElse(List.of(-1L));
        }

        String term = StringUtils.hasText(filter.search()) ? filter.search().trim() : null;
        String brand = StringUtils.hasText(filter.brand()) ? filter.brand().trim() : null;

        Page<ProductCardResponse> result = productRepository.browse(
                        term, filterCategory, categoryIds, brand,
                        filter.minPrice(), filter.maxPrice(), pageable)
                .map(productMapper::toCard);
        return PageResponse.from(result);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(String slug) {
        Product product = productRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + slug));
        return productMapper.toDetail(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCardResponse> getRelatedProducts(Long categoryId, Long excludeProductId) {
        return productRepository
                .findTop4ByActiveTrueAndCategoryIdAndIdNotOrderByCreatedAtDescIdDesc(
                        categoryId, excludeProductId)
                .stream().map(productMapper::toCard).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCardResponse> getFeaturedProducts() {
        return productRepository
                .findTop8ByActiveTrueAndDiscountPriceIsNotNullOrderByCreatedAtDescIdDesc()
                .stream().map(productMapper::toCard).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCardResponse> getNewProducts() {
        return productRepository.findTop8ByActiveTrueOrderByCreatedAtDescIdDesc()
                .stream().map(productMapper::toCard).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCardResponse> getPopularProducts() {
        return productRepository.findTop8ByActiveTrueOrderByStockQuantityDescIdDesc()
                .stream().map(productMapper::toCard).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getActiveBrands() {
        return productRepository.findActiveBrands();
    }

    /** Ids of a category and all its descendants (for subtree filtering). */
    private Set<Long> collectSubtreeIds(Long rootId) {
        var all = categoryRepository.findAll();
        Set<Long> subtree = new HashSet<>();
        Deque<Long> pending = new ArrayDeque<>();
        pending.push(rootId);
        while (!pending.isEmpty()) {
            Long currentId = pending.pop();
            if (!subtree.add(currentId)) {
                continue;
            }
            all.stream()
                    .filter(c -> c.getParentCategory() != null
                            && currentId.equals(c.getParentCategory().getId()))
                    .forEach(c -> pending.push(c.getId()));
        }
        return subtree;
    }

    // ------------------------------------------------------------------

    private Sort resolveSort(String sort) {
        return switch (sort == null ? "newest" : sort) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price").and(Sort.by("id"));
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price").and(Sort.by("id"));
            default -> Sort.by(Sort.Direction.DESC, "createdAt")
                    .and(Sort.by(Sort.Direction.DESC, "id"));
        };
    }

    private void validatePricing(ProductRequest request) {
        BigDecimal discount = request.getDiscountPrice();
        if (discount != null && discount.compareTo(request.getPrice()) >= 0) {
            throw new BusinessValidationException(
                    "Discount price must be lower than the regular price");
        }
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id));
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + categoryId));
    }

    private String generateUniqueSlug(String name, Long currentId) {
        String base = slugify(name);
        String candidate = base;
        int counter = 2;
        while (currentId == null
                ? productRepository.existsBySlug(candidate)
                : productRepository.existsBySlugAndIdNot(candidate, currentId)) {
            candidate = base + "-" + counter++;
        }
        return candidate;
    }

    private String slugify(String input) {
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return slug.isEmpty() ? "product" : slug;
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
