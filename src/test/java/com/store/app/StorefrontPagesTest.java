package com.store.app;

import com.store.app.category.dto.CategoryRequest;
import com.store.app.category.dto.CategoryResponse;
import com.store.app.category.service.CategoryService;
import com.store.app.product.dto.ProductRequest;
import com.store.app.product.dto.ProductResponse;
import com.store.app.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Renders every public storefront page against the full application
 * context (H2 in-memory database), so template errors fail the build.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StorefrontPagesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    private String productSlug;

    @BeforeEach
    void seedCatalog() {
        CategoryRequest categoryRequest = new CategoryRequest();
        categoryRequest.setName("Grocery " + System.nanoTime());
        categoryRequest.setActive(true);
        CategoryResponse category = categoryService.createCategory(categoryRequest);

        ProductRequest productRequest = new ProductRequest();
        productRequest.setProductName("Basmati Rice 5kg " + System.nanoTime());
        productRequest.setCategoryId(category.id());
        productRequest.setBrand("Daily Harvest");
        productRequest.setSku("SKU-" + System.nanoTime());
        productRequest.setPrice(new BigDecimal("499.00"));
        productRequest.setDiscountPrice(new BigDecimal("449.00"));
        productRequest.setStockQuantity(25);
        productRequest.setMinimumStockLevel(5);
        productRequest.setActive(true);
        ProductResponse product = productService.createProduct(productRequest);
        productSlug = product.slug();
    }

    @Test
    void homePageRenders() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("General Store")));
    }

    @Test
    void productListingRendersWithFilters() throws Exception {
        mockMvc.perform(get("/products")
                        .param("search", "rice")
                        .param("sort", "price_asc")
                        .param("minPrice", "10")
                        .param("maxPrice", "1000"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Shop")));
    }

    @Test
    void productDetailRendersLoginPromptForAnonymous() throws Exception {
        mockMvc.perform(get("/products/" + productSlug))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Login to buy")));
    }

    @Test
    void unknownProductReturnsError() throws Exception {
        mockMvc.perform(get("/products/does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void authPagesRender() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/register")).andExpect(status().isOk());
    }
}
