package com.store.app.category.mapper;

import com.store.app.category.dto.CategoryResponse;
import com.store.app.category.dto.CategoryTreeResponse;
import com.store.app.category.entity.Category;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps between {@link Category} entities and category DTOs.
 */
@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        Category parent = category.getParentCategory();
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getImage(),
                category.isActive(),
                parent != null ? parent.getId() : null,
                parent != null ? parent.getName() : null,
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    /**
     * Maps a category and its descendants recursively, pruning
     * inactive branches (an inactive node hides its whole subtree).
     */
    public CategoryTreeResponse toTree(Category category) {
        List<CategoryTreeResponse> children = category.getChildren().stream()
                .filter(Category::isActive)
                .map(this::toTree)
                .toList();

        return new CategoryTreeResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getImage(),
                children
        );
    }
}
