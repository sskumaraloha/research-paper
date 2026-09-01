package com.store.app.category.dto;

import java.util.List;

/**
 * Recursive category tree node for the public storefront API.
 * Contains only active categories.
 */
public record CategoryTreeResponse(
        Long id,
        String name,
        String slug,
        String description,
        String image,
        List<CategoryTreeResponse> children
) {
}
