package com.store.app;

import com.store.app.address.dto.AddressRequest;
import com.store.app.address.service.AddressService;
import com.store.app.auth.dto.RegistrationRequest;
import com.store.app.auth.service.AuthService;
import com.store.app.cart.dto.AddToCartRequest;
import com.store.app.cart.service.CartService;
import com.store.app.category.dto.CategoryRequest;
import com.store.app.category.service.CategoryService;
import com.store.app.exception.OperationNotAllowedException;
import com.store.app.inventory.service.InventoryService;
import com.store.app.order.dto.AdminOrderDetailResponse;
import com.store.app.order.dto.PlaceOrderRequest;
import com.store.app.order.entity.OrderStatus;
import com.store.app.order.service.AdminOrderService;
import com.store.app.order.service.OrderService;
import com.store.app.order.service.OrderStatusTransitionService;
import com.store.app.payment.entity.PaymentMethod;
import com.store.app.payment.entity.PaymentStatus;
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
 * Verifies the order state machine: legal chains work (with COD
 * collection on delivery), illegal jumps are rejected, admin
 * cancellation restocks, and filters find orders.
 */
@SpringBootTest
@ActiveProfiles("test")
class AdminOrderTest {

    @Autowired
    private AdminOrderService adminOrderService;
    @Autowired
    private OrderStatusTransitionService transitionService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private CartService cartService;
    @Autowired
    private AddressService addressService;
    @Autowired
    private AuthService authService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private ProductService productService;

    private Long userId;
    private Long addressId;
    private Long productId;

    @BeforeEach
    void seed() {
        long nonce = System.nanoTime();

        RegistrationRequest registration = new RegistrationRequest();
        registration.setFirstName("Fleet");
        registration.setLastName("Manager");
        registration.setPhoneNumber(String.valueOf(9100000000L + (nonce % 100000000L)));
        registration.setEmail("fleet" + nonce + "@example.com");
        registration.setPassword("Secret@123");
        registration.setConfirmPassword("Secret@123");
        userId = authService.registerCustomer(registration).id();

        AddressRequest address = new AddressRequest();
        address.setFullName("Fleet Manager");
        address.setPhoneNumber("9876504321");
        address.setAddressLine1("21 Depot Road");
        address.setCity("Thane");
        address.setState("Maharashtra");
        address.setPincode("400601");
        address.setCountry("India");
        addressId = addressService.createAddress(userId, address).id();

        CategoryRequest category = new CategoryRequest();
        category.setName("AO Cat " + nonce);
        Long categoryId = categoryService.createCategory(category).id();

        ProductRequest product = new ProductRequest();
        product.setProductName("AO Product " + nonce);
        product.setCategoryId(categoryId);
        product.setSku("AO-" + nonce);
        product.setPrice(new BigDecimal("60.00"));
        product.setStockQuantity(10);
        product.setMinimumStockLevel(1);
        product.setActive(true);
        productId = productService.createProduct(product).id();
    }

    @Test
    void transitionMapAllowsChainAndForbidsJumps() {
        assertThat(transitionService.canTransition(OrderStatus.PENDING, OrderStatus.CONFIRMED)).isTrue();
        assertThat(transitionService.canTransition(OrderStatus.PACKED, OrderStatus.SHIPPED)).isTrue();

        // Skips, reversals, and terminal states are all rejected.
        assertThat(transitionService.canTransition(OrderStatus.PENDING, OrderStatus.DELIVERED)).isFalse();
        assertThat(transitionService.canTransition(OrderStatus.SHIPPED, OrderStatus.PACKED)).isFalse();
        assertThat(transitionService.canTransition(OrderStatus.DELIVERED, OrderStatus.SHIPPED)).isFalse();
        assertThat(transitionService.canTransition(OrderStatus.CANCELLED, OrderStatus.CONFIRMED)).isFalse();
        assertThat(transitionService.canTransition(OrderStatus.PACKED, OrderStatus.CANCELLED)).isFalse();
    }

    @Test
    void fullChainDeliversAndCollectsCodPayment() {
        Long orderId = placeOrder(2);

        // COD orders start CONFIRMED; walk the legal chain to DELIVERED.
        adminOrderService.updateStatus(orderId, OrderStatus.PROCESSING);
        adminOrderService.updateStatus(orderId, OrderStatus.PACKED);
        adminOrderService.updateStatus(orderId, OrderStatus.SHIPPED);
        AdminOrderDetailResponse delivered =
                adminOrderService.updateStatus(orderId, OrderStatus.DELIVERED);

        assertThat(delivered.order().status()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(delivered.order().paymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(delivered.allowedNextStatuses()).isEmpty();
    }

    @Test
    void illegalJumpIsRejected() {
        Long orderId = placeOrder(1);

        // CONFIRMED -> DELIVERED skips three steps.
        assertThatThrownBy(() -> adminOrderService.updateStatus(orderId, OrderStatus.DELIVERED))
                .isInstanceOf(OperationNotAllowedException.class)
                .hasMessageContaining("Allowed next statuses");

        assertThat(adminOrderService.getOrder(orderId).order().status())
                .isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void adminCancellationRestocksAndSettlesPayment() {
        Long orderId = placeOrder(3);
        assertThat(inventoryService.getByProductId(productId).currentStock()).isEqualTo(7);

        adminOrderService.updateStatus(orderId, OrderStatus.PROCESSING);
        AdminOrderDetailResponse cancelled =
                adminOrderService.updateStatus(orderId, OrderStatus.CANCELLED);

        assertThat(cancelled.order().status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.order().paymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(inventoryService.getByProductId(productId).currentStock()).isEqualTo(10);
    }

    @Test
    void searchFindsOrdersByStatusAndText() {
        Long orderId = placeOrder(1);
        String orderNumber = adminOrderService.getOrder(orderId).order().orderNumber();

        var byStatus = adminOrderService.searchOrders(
                null, OrderStatus.CONFIRMED, null, null, 0, 50);
        assertThat(byStatus.content())
                .anyMatch(row -> row.orderNumber().equals(orderNumber));

        var byText = adminOrderService.searchOrders(
                orderNumber, null, null, null, 0, 10);
        assertThat(byText.totalElements()).isEqualTo(1);

        var noMatch = adminOrderService.searchOrders(
                orderNumber, OrderStatus.DELIVERED, null, null, 0, 10);
        assertThat(noMatch.totalElements()).isZero();
    }

    // ------------------------------------------------------------------

    private Long placeOrder(int quantity) {
        AddToCartRequest add = new AddToCartRequest();
        add.setProductId(productId);
        add.setQuantity(quantity);
        cartService.addToCart(userId, add);

        PlaceOrderRequest place = new PlaceOrderRequest();
        place.setAddressId(addressId);
        place.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        return orderService.placeOrder(userId, place).id();
    }
}
