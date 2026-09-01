package com.store.app.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Input for creating or updating a product. The slug is generated
 * server-side from the product name; images are uploaded separately.
 * Mutable class because it also backs the admin Thymeleaf form.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 150, message = "Product name must be between 2 and 150 characters")
    private String productName;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Category is required")
    private Long categoryId;

    @Size(max = 100, message = "Brand must not exceed 100 characters")
    private String brand;

    @NotBlank(message = "SKU is required")
    @Size(min = 2, max = 50, message = "SKU must be between 2 and 50 characters")
    private String sku;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 2 decimal places")
    private BigDecimal price;

    @DecimalMin(value = "0.01", message = "Discount price must be greater than zero")
    @Digits(integer = 8, fraction = 2, message = "Discount price must have at most 2 decimal places")
    private BigDecimal discountPrice;

    @DecimalMin(value = "0.00", message = "Cost price cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Cost price must have at most 2 decimal places")
    private BigDecimal costPrice;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @NotNull(message = "Minimum stock level is required")
    @Min(value = 0, message = "Minimum stock level cannot be negative")
    private Integer minimumStockLevel;

    private boolean active = true;
}
