package com.store.app;

import com.store.app.address.dto.AddressRequest;
import com.store.app.address.service.AddressService;
import com.store.app.admin.dto.AdminDashboardResponse;
import com.store.app.admin.service.AdminDashboardService;
import com.store.app.auth.dto.RegistrationRequest;
import com.store.app.auth.service.AuthService;
import com.store.app.cart.dto.AddToCartRequest;
import com.store.app.cart.service.CartService;
import com.store.app.category.dto.CategoryRequest;
import com.store.app.category.service.CategoryService;
import com.store.app.order.dto.OrderResponse;
import com.store.app.order.dto.PlaceOrderRequest;
import com.store.app.order.service.OrderService;
import com.store.app.payment.entity.PaymentMethod;
import com.store.app.product.dto.ProductRequest;
import com.store.app.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the admin dashboard's access rules (admin only) and its
 * headline calculations.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminDashboardTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AdminDashboardService dashboardService;
    @Autowired
    private AuthService authService;
    @Autowired
    private AddressService addressService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private ProductService productService;
    @Autowired
    private CartService cartService;
    @Autowired
    private OrderService orderService;

    @Test
    void adminCanOpenDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void customerIsDeniedWithForbidden() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                        .with(SecurityMockMvcRequestPostProcessors.user("cust").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void dashboardCountsSalesOrdersAndTopProducts() {
        AdminDashboardResponse before = dashboardService.getDashboard();

        long nonce = System.nanoTime();
        Long userId = registerCustomer(nonce);
        Long addressId = addressService.createAddress(userId, addressRequest()).id();
        Long productId = seedProduct(nonce);

        AddToCartRequest add = new AddToCartRequest();
        add.setProductId(productId);
        add.setQuantity(3);
        cartService.addToCart(userId, add);

        PlaceOrderRequest place = new PlaceOrderRequest();
        place.setAddressId(addressId);
        place.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        OrderResponse order = orderService.placeOrder(userId, place);

        AdminDashboardResponse after = dashboardService.getDashboard();

        assertThat(after.stats().totalOrders())
                .isEqualTo(before.stats().totalOrders() + 1);
        assertThat(after.stats().todaysOrders())
                .isEqualTo(before.stats().todaysOrders() + 1);
        assertThat(after.stats().totalSales())
                .isEqualByComparingTo(before.stats().totalSales().add(order.totalAmount()));
        assertThat(after.stats().totalCustomers())
                .isEqualTo(before.stats().totalCustomers() + 1);

        assertThat(after.recentOrders())
                .anyMatch(row -> row.orderNumber().equals(order.orderNumber()));
        assertThat(after.topProducts())
                .anyMatch(row -> row.unitsSold() >= 3);

        // A cancelled order leaves total sales unchanged relative to itself.
        orderService.cancelOrder(userId, order.id());
        AdminDashboardResponse afterCancel = dashboardService.getDashboard();
        assertThat(afterCancel.stats().totalSales())
                .isEqualByComparingTo(before.stats().totalSales());
        assertThat(afterCancel.stats().totalOrders())
                .isEqualTo(before.stats().totalOrders() + 1);
    }

    // ------------------------------------------------------------------

    private Long registerCustomer(long nonce) {
        RegistrationRequest registration = new RegistrationRequest();
        registration.setFirstName("Dash");
        registration.setLastName("Tester");
        registration.setPhoneNumber(String.valueOf(9000000000L + (nonce % 1000000000L)));
        registration.setEmail("dash" + nonce + "@example.com");
        registration.setPassword("Secret@123");
        registration.setConfirmPassword("Secret@123");
        return authService.registerCustomer(registration).id();
    }

    private AddressRequest addressRequest() {
        AddressRequest request = new AddressRequest();
        request.setFullName("Dash Tester");
        request.setPhoneNumber("9876512345");
        request.setAddressLine1("3 Dashboard Lane");
        request.setCity("Mumbai");
        request.setState("Maharashtra");
        request.setPincode("400001");
        request.setCountry("India");
        return request;
    }

    private Long seedProduct(long nonce) {
        CategoryRequest category = new CategoryRequest();
        category.setName("Dash Cat " + nonce);
        Long categoryId = categoryService.createCategory(category).id();

        ProductRequest product = new ProductRequest();
        product.setProductName("Dash Product " + nonce);
        product.setCategoryId(categoryId);
        product.setSku("DASH-" + nonce);
        product.setPrice(new BigDecimal("40.00"));
        product.setStockQuantity(20);
        product.setMinimumStockLevel(2);
        product.setActive(true);
        return productService.createProduct(product).id();
    }
}
