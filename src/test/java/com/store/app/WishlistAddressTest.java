package com.store.app;

import com.store.app.address.dto.AddressRequest;
import com.store.app.address.dto.AddressResponse;
import com.store.app.address.service.AddressService;
import com.store.app.auth.dto.RegistrationRequest;
import com.store.app.auth.service.AuthService;
import com.store.app.cart.service.CartService;
import com.store.app.category.dto.CategoryRequest;
import com.store.app.category.service.CategoryService;
import com.store.app.exception.DuplicateResourceException;
import com.store.app.exception.ResourceNotFoundException;
import com.store.app.product.dto.ProductRequest;
import com.store.app.product.service.ProductService;
import com.store.app.wishlist.dto.WishlistItemResponse;
import com.store.app.wishlist.service.WishlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies wishlist behavior (uniqueness, move-to-cart) and address
 * behavior (default logic, strict per-user ownership).
 */
@SpringBootTest
@ActiveProfiles("test")
class WishlistAddressTest {

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private AddressService addressService;

    @Autowired
    private CartService cartService;

    @Autowired
    private AuthService authService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    private Long userA;
    private Long userB;
    private Long productId;

    @BeforeEach
    void seed() {
        userA = registerUser();
        userB = registerUser();

        CategoryRequest category = new CategoryRequest();
        category.setName("WL Cat " + System.nanoTime());
        Long categoryId = categoryService.createCategory(category).id();

        ProductRequest product = new ProductRequest();
        product.setProductName("WL Product " + System.nanoTime());
        product.setCategoryId(categoryId);
        product.setSku("WL-" + System.nanoTime());
        product.setPrice(new BigDecimal("25.00"));
        product.setStockQuantity(4);
        product.setMinimumStockLevel(1);
        product.setActive(true);
        productId = productService.createProduct(product).id();
    }

    @Test
    void wishlistAddRemoveAndDuplicateRejection() {
        WishlistItemResponse item = wishlistService.addProduct(userA, productId);
        assertThat(wishlistService.getWishlist(userA)).hasSize(1);

        assertThatThrownBy(() -> wishlistService.addProduct(userA, productId))
                .isInstanceOf(DuplicateResourceException.class);

        wishlistService.removeItem(userA, item.id());
        assertThat(wishlistService.getWishlist(userA)).isEmpty();
    }

    @Test
    void moveToCartAddsToCartAndRemovesFromWishlist() {
        WishlistItemResponse item = wishlistService.addProduct(userA, productId);
        wishlistService.moveToCart(userA, item.id());

        assertThat(wishlistService.getWishlist(userA)).isEmpty();
        assertThat(cartService.getCart(userA).totalItems()).isEqualTo(1);
    }

    @Test
    void wishlistItemsOfAnotherUserAreUnreachable() {
        WishlistItemResponse item = wishlistService.addProduct(userA, productId);

        assertThatThrownBy(() -> wishlistService.removeItem(userB, item.id()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(wishlistService.getWishlist(userA)).hasSize(1);
    }

    @Test
    void firstAddressBecomesDefaultAndDefaultMoves() {
        AddressResponse first = addressService.createAddress(userA, addressRequest("First", false));
        assertThat(first.defaultAddress()).isTrue();

        AddressResponse second = addressService.createAddress(userA, addressRequest("Second", true));
        assertThat(second.defaultAddress()).isTrue();

        List<AddressResponse> addresses = addressService.getAddresses(userA);
        assertThat(addresses).hasSize(2);
        assertThat(addresses.stream().filter(AddressResponse::defaultAddress)).hasSize(1);
        assertThat(addresses.get(0).fullName()).isEqualTo("Second");
    }

    @Test
    void deletingDefaultPromotesAnotherAddress() {
        AddressResponse first = addressService.createAddress(userA, addressRequest("First", false));
        addressService.createAddress(userA, addressRequest("Second", false));

        addressService.deleteAddress(userA, first.id());

        List<AddressResponse> remaining = addressService.getAddresses(userA);
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).defaultAddress()).isTrue();
    }

    @Test
    void addressesOfAnotherUserAreUnreachable() {
        AddressResponse address = addressService.createAddress(userA, addressRequest("Mine", true));

        assertThatThrownBy(() -> addressService.getAddress(userB, address.id()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> addressService.deleteAddress(userB, address.id()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> addressService.setDefaultAddress(userB, address.id()))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(addressService.getAddresses(userA)).hasSize(1);
    }

    // ------------------------------------------------------------------

    private Long registerUser() {
        long nonce = System.nanoTime();
        RegistrationRequest registration = new RegistrationRequest();
        registration.setFirstName("Wish");
        registration.setLastName("Tester");
        registration.setPhoneNumber(String.valueOf(7000000000L + (nonce % 1000000000L)));
        registration.setEmail("wish" + nonce + "@example.com");
        registration.setPassword("Secret@123");
        registration.setConfirmPassword("Secret@123");
        return authService.registerCustomer(registration).id();
    }

    private AddressRequest addressRequest(String fullName, boolean defaultAddress) {
        AddressRequest request = new AddressRequest();
        request.setFullName(fullName);
        request.setPhoneNumber("9876543210");
        request.setAddressLine1("12 Market Road");
        request.setCity("Pune");
        request.setState("Maharashtra");
        request.setPincode("411001");
        request.setCountry("India");
        request.setDefaultAddress(defaultAddress);
        return request;
    }
}
