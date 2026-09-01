package com.store.app.product.mapper;

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
}
