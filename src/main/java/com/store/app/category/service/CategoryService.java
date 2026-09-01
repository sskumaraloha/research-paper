package com.store.app.category.service;

import com.store.app.category.dto.CategoryOptionResponse;
import com.store.app.category.dto.CategoryRequest;
import com.store.app.category.dto.CategoryResponse;
import com.store.app.category.dto.CategoryTreeResponse;

import java.util.List;

/**
 * Category management (admin) and public catalog browsing.
 */
public interface CategoryService {

    /**
     * Creates a category; with a {@code parentId} this creates a subcategory.
     *
     * @throws com.store.app.exception.ResourceNotFoundException
     *         if the parent does not exist
     */
    CategoryResponse createCategory(CategoryRequest request);

    /**
     * Updates a category, including moving it to another parent.
     *
     * @throws com.store.app.exception.InvalidCategoryHierarchyException
     *         if the new parent is the category itself or one of its descendants
     */
    CategoryResponse updateCategory(Long id, CategoryRequest request);

    /**
     * Deletes a category.
     *
     * @throws com.store.app.exception.OperationNotAllowedException
     *         if the category still has subcategories
     */
    void deleteCategory(Long id);

    CategoryResponse setCategoryActive(Long id, boolean active);

    CategoryResponse getCategoryById(Long id);

    /** All categories, flat, for the admin listing. */
    List<CategoryResponse> getAllCategories();

    /**
     * Categories eligible as parent of the given category (all except the
     * category itself and its descendants). Pass {@code null} when creating.
     */
    List<CategoryResponse> getParentOptions(Long excludeId);

    /** Public storefront tree: active categories only, inactive branches pruned. */
    List<CategoryTreeResponse> getActiveCategoryTree();

    /** Active categories flattened depth-first, labels indented by depth. */
    List<CategoryOptionResponse> getActiveCategoryOptions();

    /**
     * Public lookup of one active category (with its active subtree).
     *
     * @throws com.store.app.exception.ResourceNotFoundException
     *         if no active category has this slug
     */
    CategoryTreeResponse getActiveCategoryBySlug(String slug);
}
