package com.store.app;

import com.store.app.address.dto.AddressRequest;
import com.store.app.address.service.AddressService;
import com.store.app.auth.dto.RegistrationRequest;
import com.store.app.auth.service.AuthService;
import com.store.app.cart.dto.AddToCartRequest;
import com.store.app.cart.service.CartService;
import com.store.app.category.dto.CategoryRequest;
import com.store.app.category.service.CategoryService;
import com.store.app.exception.BusinessValidationException;
import com.store.app.exception.ResourceNotFoundException;
import com.store.app.inventory.dto.AdjustStockRequest;
import com.store.app.inventory.entity.InventoryTransactionType;
import com.store.app.inventory.service.InventoryService;
import com.store.app.order.dto.OrderResponse;
import com.store.app.order.dto.PlaceOrderRequest;
import com.store.app.order.entity.OrderStatus;
import com.store.app.order.service.OrderService;
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
 * Verifies the checkout transaction: inventory reduction with audit
 * records, cart clearing, snapshots, all-or-nothing rollback on
 * oversell, cancellation restock, and address ownership.
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderServiceTest {

    @Autowired
    private OrderService orderService;
    @Autowired
    private CartService cartService;
    @Autowired
    private InventoryService inventoryService;
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
    private Long dealProductId;   // price 100.00, discount 80.00, stock 10
    private Long plainProductId;  // price 50.00, stock 3

    @BeforeEach
    void seed() {
        long nonce = System.nanoTime();
        userId = registerUser(nonce);
        addressId = addressService.createAddress(userId, addressRequest()).id();

        CategoryRequest category = new CategoryRequest();
        category.setName("Order Cat " + nonce);
        Long categoryId = categoryService.createCategory(category).id();

        dealProductId = createProduct(categoryId, "Deal " + nonce,
                new BigDecimal("100.00"), new BigDecimal("80.00"), 10);
        plainProductId = createProduct(categoryId, "Plain " + nonce,
                new BigDecimal("50.00"), null, 3);
    }

    @Test
    void placeOrderReducesInventoryClearsCartAndSnapshotsPrices() {
        cartService.addToCart(userId, addRequest(dealProductId, 2));
        cartService.addToCart(userId, addRequest(plainProductId, 1));

        OrderResponse order = orderService.placeOrder(userId, placeRequest(
                PaymentMethod.CASH_ON_DELIVERY));

        // Money: subtotal 2*100+50=250, total 2*80+50=210, discount 40.
        assertThat(order.subtotal()).isEqualByComparingTo("250.00");
        assertThat(order.discount()).isEqualByComparingTo("40.00");
        assertThat(order.totalAmount()).isEqualByComparingTo("210.00");
        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.paymentStatus()).isEqualTo(PaymentStatus.PENDING);

        // Inventory reduced with SALE transactions referencing the order.
        assertThat(inventoryService.getByProductId(dealProductId).currentStock()).isEqualTo(8);
        assertThat(inventoryService.getByProductId(plainProductId).currentStock()).isEqualTo(2);
        var latestTxn = inventoryService.getTransactions(dealProductId, 0, 1).content().get(0);
        assertThat(latestTxn.transactionType()).isEqualTo(InventoryTransactionType.SALE);
        assertThat(latestTxn.reference()).isEqualTo(order.orderNumber());

        // Cart cleared.
        assertThat(cartService.getCart(userId).items()).isEmpty();

        // Snapshot survives a later price change.
        BigDecimal snapshotPrice = order.items().stream()
                .filter(i -> i.productId().equals(dealProductId))
                .findFirst().orElseThrow().priceAtPurchase();
        assertThat(snapshotPrice).isEqualByComparingTo("80.00");

        var edit = productService.getProductById(dealProductId);
        ProductRequest update = new ProductRequest();
        update.setProductName(edit.productName());
        update.setCategoryId(edit.categoryId());
        update.setSku(edit.sku());
        update.setPrice(new BigDecimal("999.00"));
        update.setStockQuantity(edit.stockQuantity());
        update.setMinimumStockLevel(edit.minimumStockLevel());
        update.setActive(true);
        productService.updateProduct(dealProductId, update);

        OrderResponse reloaded = orderService.getOrder(userId, order.id());
        assertThat(reloaded.items().stream()
                .filter(i -> i.productId().equals(dealProductId))
                .findFirst().orElseThrow().priceAtPurchase())
                .isEqualByComparingTo("80.00");
    }

    @Test
    void oversellRejectsTheWholeOrderWithoutSideEffects() {
        cartService.addToCart(userId, addRequest(dealProductId, 2));
        cartService.addToCart(userId, addRequest(plainProductId, 3));

        // Stock of the second product drops below the carted quantity
        // after it was added (e.g. a concurrent sale).
        AdjustStockRequest adjust = new AdjustStockRequest();
        adjust.setProductId(plainProductId);
        adjust.setNewStock(1);
        adjust.setRemarks("Concurrent sale simulation");
        inventoryService.adjustStock(adjust);

        assertThatThrownBy(() -> orderService.placeOrder(
                userId, placeRequest(PaymentMethod.CASH_ON_DELIVERY)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Insufficient stock");

        // Nothing happened: no order, cart intact, first product untouched.
        assertThat(orderService.getOrders(userId, 0, 10).totalElements()).isZero();
        assertThat(cartService.getCart(userId).items()).hasSize(2);
        assertThat(inventoryService.getByProductId(dealProductId).currentStock()).isEqualTo(10);
        assertThat(inventoryService.getByProductId(plainProductId).currentStock()).isEqualTo(1);
    }

    @Test
    void cancellingRestocksAndSettlesPayment() {
        cartService.addToCart(userId, addRequest(plainProductId, 2));
        OrderResponse order = orderService.placeOrder(
                userId, placeRequest(PaymentMethod.CASH_ON_DELIVERY));
        assertThat(inventoryService.getByProductId(plainProductId).currentStock()).isEqualTo(1);

        OrderResponse cancelled = orderService.cancelOrder(userId, order.id());
        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.paymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(inventoryService.getByProductId(plainProductId).currentStock()).isEqualTo(3);

        var latestTxn = inventoryService.getTransactions(plainProductId, 0, 1).content().get(0);
        assertThat(latestTxn.transactionType()).isEqualTo(InventoryTransactionType.RETURN);
    }

    @Test
    void anotherUsersAddressIsRejected() {
        Long otherUser = registerUser(System.nanoTime());
        Long foreignAddressId = addressService.createAddress(otherUser, addressRequest()).id();

        cartService.addToCart(userId, addRequest(plainProductId, 1));
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setAddressId(foreignAddressId);
        request.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);

        assertThatThrownBy(() -> orderService.placeOrder(userId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void onlinePaymentIsRejectedUntilAGatewayExists() {
        cartService.addToCart(userId, addRequest(plainProductId, 1));

        assertThatThrownBy(() -> orderService.placeOrder(
                userId, placeRequest(PaymentMethod.ONLINE)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("not available yet");

        // Fully rolled back: no order, cart intact, stock untouched.
        assertThat(orderService.getOrders(userId, 0, 10).totalElements()).isZero();
        assertThat(cartService.getCart(userId).items()).hasSize(1);
        assertThat(inventoryService.getByProductId(plainProductId).currentStock()).isEqualTo(3);
    }

    @Test
    void emptyCartCannotCheckOut() {
        assertThatThrownBy(() -> orderService.placeOrder(
                userId, placeRequest(PaymentMethod.CASH_ON_DELIVERY)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("cart is empty");
    }

    // ------------------------------------------------------------------

    private Long registerUser(long nonce) {
        RegistrationRequest registration = new RegistrationRequest();
        registration.setFirstName("Order");
        registration.setLastName("Tester");
        registration.setPhoneNumber(String.valueOf(8000000000L + (nonce % 1000000000L)));
        registration.setEmail("order" + nonce + "@example.com");
        registration.setPassword("Secret@123");
        registration.setConfirmPassword("Secret@123");
        return authService.registerCustomer(registration).id();
    }

    private AddressRequest addressRequest() {
        AddressRequest request = new AddressRequest();
        request.setFullName("Order Tester");
        request.setPhoneNumber("9876501234");
        request.setAddressLine1("7 Bazaar Street");
        request.setCity("Nashik");
        request.setState("Maharashtra");
        request.setPincode("422001");
        request.setCountry("India");
        return request;
    }

    private PlaceOrderRequest placeRequest(PaymentMethod method) {
        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setAddressId(addressId);
        request.setPaymentMethod(method);
        return request;
    }

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
        request.setSku("ORD-" + System.nanoTime());
        request.setPrice(price);
        request.setDiscountPrice(discountPrice);
        request.setStockQuantity(stock);
        request.setMinimumStockLevel(1);
        request.setActive(true);
        return productService.createProduct(request).id();
    }
}
