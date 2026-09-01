package com.store.app.category.controller;

import com.store.app.category.dto.CategoryTreeResponse;
import com.store.app.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public catalog API: exposes only active categories, with inactive
 * branches pruned from the tree.
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryTreeResponse>> getCategoryTree() {
        return ResponseEntity.ok(categoryService.getActiveCategoryTree());
    }

    @GetMapping("/{slug}")
    public ResponseEntity<CategoryTreeResponse> getCategoryBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(categoryService.getActiveCategoryBySlug(slug));
    }
}
