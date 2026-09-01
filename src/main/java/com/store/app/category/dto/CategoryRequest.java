package com.store.app.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Input for creating or updating a category. A non-null {@code parentId}
 * makes it a subcategory of that parent; null makes it a root category.
 * The slug is generated server-side from the name.
 * Mutable class because it also backs the admin Thymeleaf form.
 */
@Getter
@Setter
@NoArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 255, message = "Image URL must not exceed 255 characters")
    private String image;

    /** Parent category id, or null for a root category. */
    private Long parentId;

    private boolean active = true;
}
