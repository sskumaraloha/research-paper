package com.store.app.product.mapper;

import com.store.app.product.dto.ProductCardResponse;
import com.store.app.product.dto.ProductDetailResponse;
import com.store.app.product.dto.ProductImageResponse;
import com.store.app.product.dto.ProductResponse;
import com.store.app.product.entity.Product;
import com.store.app.product.entity.ProductImage;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps between {@link Product} entities and product DTOs.
 */
@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        List<ProductImageResponse> images = product.getImages().stream()
                .map(this::toImageResponse)
                .toList();

        return new ProductResponse(
                product.getId(),
                product.getProductName(),
                product.getSlug(),
                product.getDescription(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getBrand(),
                product.getSku(),
                product.getPrice(),
                product.getDiscountPrice(),
                product.getCostPrice(),
                product.getStockQuantity(),
                product.getMinimumStockLevel(),
                product.isActive(),
                images,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public ProductImageResponse toImageResponse(ProductImage image) {
        return new ProductImageResponse(
                image.getId(),
                image.getImageUrl(),
                image.getDisplayOrder()
        );
    }

    /** Customer-facing card: first image as thumbnail, no internal figures. */
    public ProductCardResponse toCard(Product product) {
        String imageUrl = product.getImages().isEmpty()
                ? null
                : product.getImages().get(0).getImageUrl();

        return new ProductCardResponse(
                product.getId(),
                product.getProductName(),
                product.getSlug(),
                product.getBrand(),
                product.getPrice(),
                product.getDiscountPrice(),
                imageUrl,
                product.getStockQuantity() > 0
        );
    }

    /** Customer-facing detail: coarse availability instead of stock numbers. */
    public ProductDetailResponse toDetail(Product product) {
        return new ProductDetailResponse(
                product.getId(),
                product.getProductName(),
                product.getSlug(),
                product.getDescription(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCategory().getSlug(),
                product.getBrand(),
                product.getPrice(),
                product.getDiscountPrice(),
                availabilityOf(product),
                product.getImages().stream().map(this::toImageResponse).toList()
        );
    }

    private ProductDetailResponse.StockAvailability availabilityOf(Product product) {
        if (product.getStockQuantity() <= 0) {
            return ProductDetailResponse.StockAvailability.OUT_OF_STOCK;
        }
        if (product.getStockQuantity() <= product.getMinimumStockLevel()) {
            return ProductDetailResponse.StockAvailability.LOW_STOCK;
        }
        return ProductDetailResponse.StockAvailability.IN_STOCK;
    }
}
