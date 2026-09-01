package com.store.app.customer.controller;

import com.store.app.category.service.CategoryService;
import com.store.app.common.dto.PageResponse;
import com.store.app.product.dto.ProductCardResponse;
import com.store.app.product.dto.ProductDetailResponse;
import com.store.app.product.dto.ProductFilterRequest;
import com.store.app.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/**
 * Customer-facing storefront pages: home, product listing, product detail.
 * All pages are public.
 */
@Controller
@RequiredArgsConstructor
public class StorefrontController {

    private static final int LISTING_PAGE_SIZE = 12;

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("categories", categoryService.getActiveCategoryTree());
        model.addAttribute("featuredProducts", productService.getFeaturedProducts());
        model.addAttribute("newProducts", productService.getNewProducts());
        model.addAttribute("popularProducts", productService.getPopularProducts());
        return "customer/home";
    }

    @GetMapping("/products")
    public String listProducts(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "brand", required = false) String brand,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(name = "sort", defaultValue = "newest") String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model) {

        ProductFilterRequest filter = new ProductFilterRequest(
                search, category, brand, minPrice, maxPrice, sort, page, LISTING_PAGE_SIZE);
        PageResponse<ProductCardResponse> products = productService.browseProducts(filter);

        model.addAttribute("products", products);
        model.addAttribute("categoryOptions", categoryService.getActiveCategoryOptions());
        model.addAttribute("brandOptions", productService.getActiveBrands());
        model.addAttribute("search", search);
        model.addAttribute("category", category);
        model.addAttribute("brand", brand);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sort", sort);
        return "customer/products";
    }

    @GetMapping("/products/{slug}")
    public String productDetail(@PathVariable String slug, Model model) {
        ProductDetailResponse product = productService.getProductDetail(slug);
        model.addAttribute("product", product);
        model.addAttribute("relatedProducts",
                productService.getRelatedProducts(product.categoryId(), product.id()));
        return "customer/product-detail";
    }
}
