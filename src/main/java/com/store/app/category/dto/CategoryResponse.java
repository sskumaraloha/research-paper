package com.store.app.category.dto;

import java.time.LocalDateTime;

/**
 * Flat representation of a category (admin listings, lookups).
 */
public record CategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        String image,
        boolean active,
        Long parentId,
        String parentName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
