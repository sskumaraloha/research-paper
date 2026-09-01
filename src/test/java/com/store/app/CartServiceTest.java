package com.store.app;

import com.store.app.auth.dto.RegistrationRequest;
import com.store.app.auth.service.AuthService;
import com.store.app.cart.dto.AddToCartRequest;
import com.store.app.cart.dto.CartResponse;
import com.store.app.cart.service.CartService;
import com.store.app.category.dto.CategoryRequest;
import com.store.app.category.service.CategoryService;
import com.store.app.exception.BusinessValidationException;
import com.store.app.product.dto.ProductRequest;
import com.store.app.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the cart invariants: one cart per user, stock-capped
 * quantities, and correct subtotal/discount/total math.
 */
@SpringBootTest
@ActiveProfiles("test")
class CartServiceTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private AuthService authService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    private Long userId;
    private Long discountedProductId;   // price 100.00, discount 80.00, stock 5
    private Long plainProductId;        // price 50.00, no discount, stock 2

    @BeforeEach
    void seed() {
        long nonce = System.nanoTime();

        RegistrationRequest registration = new RegistrationRequest();
        registration.setFirstName("Cart");
        registration.setLastName("Tester");
        registration.setPhoneNumber(String.valueOf(6000000000L + (nonce % 1000000000L)));
        registration.setEmail("cart" + nonce + "@example.com");
        registration.setPassword("Secret@123");
        registration.setConfirmPassword("Secret@123");
        userId = authService.registerCustomer(registration).id();

        CategoryRequest category = new CategoryRequest();
        category.setName("Cart Cat " + nonce);
        Long categoryId = categoryService.createCategory(category).id();

        discountedProductId = createProduct(categoryId, "Deal Item " + nonce,
                new BigDecimal("100.00"), new BigDecimal("80.00"), 5);
        plainProductId = createProduct(categoryId, "Plain Item " + nonce,
                new BigDecimal("50.00"), null, 2);
    }

    @Test
    void addAccumulatesAndComputesTotals() {
        cartService.addToCart(userId, addRequest(discountedProductId, 2));
        cartService.addToCart(userId, addRequest(discountedProductId, 1)); // same line: 3
        CartResponse cart = cartService.addToCart(userId, addRequest(plainProductId, 2));

        assertThat(cart.items()).hasSize(2);
        assertThat(cart.totalItems()).isEqualTo(5);
        // subtotal: 3*100 + 2*50 = 400; total: 3*80 + 2*50 = 340; discount 60
        assertThat(cart.subtotal()).isEqualByComparingTo("400.00");
        assertThat(cart.discount()).isEqualByComparingTo("60.00");
        assertThat(cart.total()).isEqualByComparingTo("340.00");
    }

    @Test
    void quantityAboveStockIsRejected() {
        assertThatThrownBy(() -> cartService.addToCart(userId, addRequest(plainProductId, 3)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Only 2");

        // Accumulation across adds is also capped.
        cartService.addToCart(userId, addRequest(plainProductId, 2));
        assertThatThrownBy(() -> cartService.addToCart(userId, addRequest(plainProductId, 1)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("already in your cart");
    }

    @Test
    void decreaseToZeroRemovesItemAndUpdateIsStockValidated() {
        CartResponse cart = cartService.addToCart(userId, addRequest(plainProductId, 1));
        Long itemId = cart.items().get(0).id();

        assertThatThrownBy(() -> cartService.updateItemQuantity(userId, itemId, 3))
                .isInstanceOf(BusinessValidationException.class);

        cart = cartService.increaseItemQuantity(userId, itemId);
        assertThat(cart.items().get(0).quantity()).isEqualTo(2);

        cart = cartService.decreaseItemQuantity(userId, itemId);
        cart = cartService.decreaseItemQuantity(userId, itemId);
        assertThat(cart.items()).isEmpty();
        assertThat(cart.total()).isEqualByComparingTo("0");
    }

    @Test
    void cartIsPerUserAndCreatedLazily() {
        CartResponse first = cartService.getCart(userId);
        CartResponse second = cartService.getCart(userId);
        assertThat(first.id()).isEqualTo(second.id());
        assertThat(first.items()).isEmpty();
    }

    // ------------------------------------------------------------------

    private AddToCartRequest addRequest(Long productId, int quantity) {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(productId);
        request.setQuantity(quantity);
        return request;
    }

    private Long createProduct(Long categoryId, String name, BigDecimal price,
                               BigDecimal discountPrice, int stock) {
        ProductRequest request = new ProductRequest();
        request.setProductName(name);
        request.setCategoryId(categoryId);
        request.setSku("CART-" + System.nanoTime());
        request.setPrice(price);
        request.setDiscountPrice(discountPrice);
        request.setStockQuantity(stock);
        request.setMinimumStockLevel(1);
        request.setActive(true);
        return productService.createProduct(request).id();
    }
}
