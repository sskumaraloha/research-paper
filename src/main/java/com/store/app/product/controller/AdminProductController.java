package com.store.app.product.controller;

import com.store.app.common.dto.PageResponse;
import com.store.app.product.dto.ProductRequest;
import com.store.app.product.dto.ProductResponse;
import com.store.app.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Admin REST API for product management. Guarded by ROLE_ADMIN
 * through the /api/admin/** security rule (JWT).
 */
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> searchProducts(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "sort", defaultValue = "newest") String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.searchProducts(search, sort, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ProductResponse> activateProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.setProductActive(id, true));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ProductResponse> deactivateProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.setProductActive(id, false));
    }

    @PostMapping("/{id}/images")
    public ResponseEntity<ProductResponse> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.addProductImage(id, file));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<ProductResponse> deleteImage(
            @PathVariable Long id, @PathVariable Long imageId) {
        return ResponseEntity.ok(productService.deleteProductImage(id, imageId));
    }
}
