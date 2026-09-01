package com.store.app.category.dto;

/**
 * Flattened active-category option for storefront filter dropdowns;
 * the label carries indentation showing tree depth.
 */
public record CategoryOptionResponse(String slug, String label) {
}
