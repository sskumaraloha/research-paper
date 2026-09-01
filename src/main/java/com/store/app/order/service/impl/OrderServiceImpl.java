package com.store.app.order.service.impl;

import com.store.app.address.dto.AddressResponse;
import com.store.app.address.service.AddressService;
import com.store.app.cart.entity.Cart;
import com.store.app.cart.entity.CartItem;
import com.store.app.cart.mapper.CartMapper;
import com.store.app.cart.repository.CartRepository;
import com.store.app.common.dto.PageResponse;
import com.store.app.exception.BusinessValidationException;
import com.store.app.exception.OperationNotAllowedException;
import com.store.app.exception.ResourceNotFoundException;
import com.store.app.inventory.dto.StockUpdateRequest;
import com.store.app.inventory.entity.InventoryTransactionType;
import com.store.app.inventory.service.InventoryService;
import com.store.app.order.dto.OrderResponse;
import com.store.app.order.dto.OrderSummaryResponse;
import com.store.app.order.dto.PlaceOrderRequest;
import com.store.app.order.entity.Order;
import com.store.app.order.entity.OrderItem;
import com.store.app.order.entity.OrderStatus;
import com.store.app.order.entity.ShippingAddress;
import com.store.app.order.mapper.OrderMapper;
import com.store.app.order.repository.OrderItemRepository;
import com.store.app.order.repository.OrderRepository;
import com.store.app.order.service.OrderService;
import com.store.app.payment.entity.Payment;
import com.store.app.payment.entity.PaymentMethod;
import com.store.app.payment.entity.PaymentStatus;
import com.store.app.product.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ORDER_NUMBER_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int MAX_PAGE_SIZE = 50;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartMapper cartMapper;
    private final AddressService addressService;
    private final InventoryService inventoryService;
    private final OrderMapper orderMapper;

    @Override
    public OrderResponse placeOrder(Long userId, PlaceOrderRequest request) {
        // 1. The cart must exist and contain items.
        Cart cart = cartRepository.findByUserId(userId)
                .filter(c -> !c.getItems().isEmpty())
                .orElseThrow(() -> new BusinessValidationException("Your cart is empty"));

        // 2. Ownership-checked address, snapshotted below.
        AddressResponse address = addressService.getAddress(userId, request.getAddressId());

        String orderNumber = generateOrderNumber();

        // 3. Reduce stock per line in deterministic product order so two
        //    concurrent checkouts lock rows in the same sequence (no
        //    deadlocks). Each call locks the inventory row, re-validates
        //    availability, and records a SALE transaction. Any failure
        //    rolls back the entire checkout.
        List<CartItem> lines = cart.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getId()))
                .toList();

        for (CartItem line : lines) {
            Product product = line.getProduct();
            if (!product.isActive()) {
                throw new BusinessValidationException(
                        "\"" + product.getProductName() + "\" is no longer available. "
                                + "Please remove it from your cart.");
            }
            StockUpdateRequest sale = new StockUpdateRequest();
            sale.setProductId(product.getId());
            sale.setQuantity(line.getQuantity());
            sale.setTransactionType(InventoryTransactionType.SALE);
            sale.setReference(orderNumber);
            sale.setRemarks("Online order");
            inventoryService.decreaseStock(sale);
        }

        // 4. Money figures from current effective prices (what the cart showed).
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem line : lines) {
            BigDecimal quantity = BigDecimal.valueOf(line.getQuantity());
            subtotal = subtotal.add(line.getProduct().getPrice().multiply(quantity));
            total = total.add(cartMapper.effectivePrice(line.getProduct()).multiply(quantity));
        }

        // 5. Order with item + address snapshots and the payment record.
        OrderStatus initialStatus = request.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY
                ? OrderStatus.CONFIRMED   // COD needs no upfront payment
                : OrderStatus.PENDING;    // ONLINE awaits payment processing

        Order order = new Order(
                orderNumber,
                cart.getUser(),
                initialStatus,
                toShippingAddress(address),
                subtotal,
                subtotal.subtract(total),
                total);

        for (CartItem line : lines) {
            Product product = line.getProduct();
            order.addItem(new OrderItem(
                    product,
                    product.getProductName(),
                    product.getSku(),
                    cartMapper.effectivePrice(product),
                    line.getQuantity()));
        }

        order.attachPayment(new Payment(
                request.getPaymentMethod(), PaymentStatus.PENDING, total));

        Order saved = orderRepository.save(order);

        // 6. Empty the cart — same transaction, so a failure restores it.
        cart.clear();
        cartRepository.save(cart);

        log.info("Order {} placed by user {} ({} items, total {})",
                orderNumber, userId, saved.getItems().size(), total);
        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getOrders(Long userId, int page, int size) {
        var pageable = PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return PageResponse.from(
                orderRepository.findAllByUserIdOrderByIdDesc(userId, pageable)
                        .map(orderMapper::toSummary));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long userId, Long orderId) {
        return orderMapper.toResponse(requiredOrder(userId, orderId));
    }

    @Override
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        Order order = requiredOrder(userId, orderId);
        if (!order.isCancellable()) {
            throw new OperationNotAllowedException(
                    "Order " + order.getOrderNumber() + " can no longer be cancelled ("
                            + order.getStatus() + ")");
        }

        // Restock every line that still maps to a catalog product.
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() == null) {
                continue;
            }
            StockUpdateRequest restock = new StockUpdateRequest();
            restock.setProductId(item.getProduct().getId());
            restock.setQuantity(item.getQuantity());
            restock.setTransactionType(InventoryTransactionType.RETURN);
            restock.setReference(order.getOrderNumber());
            restock.setRemarks("Order cancelled");
            inventoryService.increaseStock(restock);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Payment payment = order.getPayment();
        payment.setStatus(payment.getStatus() == PaymentStatus.PAID
                ? PaymentStatus.REFUNDED
                : PaymentStatus.FAILED);

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    public void detachProductFromOrders(Long productId) {
        orderItemRepository.detachProduct(productId);
    }

    // ------------------------------------------------------------------

    private Order requiredOrder(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + orderId));
    }

    private ShippingAddress toShippingAddress(AddressResponse address) {
        return new ShippingAddress(
                address.fullName(),
                address.phoneNumber(),
                address.addressLine1(),
                address.addressLine2(),
                address.city(),
                address.state(),
                address.pincode(),
                address.country());
    }

    private String generateOrderNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String candidate;
        do {
            StringBuilder suffix = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                suffix.append(ORDER_NUMBER_ALPHABET.charAt(
                        RANDOM.nextInt(ORDER_NUMBER_ALPHABET.length())));
            }
            candidate = "ORD-" + date + "-" + suffix;
        } while (orderRepository.existsByOrderNumber(candidate));
        return candidate;
    }
}
