package com.store.app.customer.controller;

import com.store.app.category.dto.CategoryTreeResponse;
import com.store.app.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * Supplies the navbar category dropdown to the storefront pages.
 * Scoped to the storefront controller so REST and admin controllers
 * don't pay for the lookup.
 */
@ControllerAdvice(assignableTypes = StorefrontController.class)
@RequiredArgsConstructor
public class StorefrontModelAdvice {

    private final CategoryService categoryService;

    @ModelAttribute("navCategories")
    public List<CategoryTreeResponse> navCategories() {
        return categoryService.getActiveCategoryTree();
    }
}
