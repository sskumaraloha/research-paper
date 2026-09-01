package com.store.app.order.entity;

import com.store.app.common.entity.BaseEntity;
import com.store.app.payment.entity.Payment;
import com.store.app.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A customer order. Items, payment, and the shipping address are all
 * point-in-time snapshots — an order's history never changes because
 * the catalog or address book changed afterwards.
 * (Table named {@code orders}: {@code order} is a reserved SQL word.)
 */
@Entity
@Table(
        name = "orders",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_orders_order_number", columnNames = "order_number"),
        indexes = @Index(name = "idx_orders_user", columnList = "user_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable unique reference, e.g. ORD-20260901-4F7K2A. */
    @Column(name = "order_number", nullable = false, length = 30)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Embedded
    private ShippingAddress shippingAddress;

    /** Sum of regular prices x quantities. */
    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discount;

    /** Payable amount: subtotal - discount. */
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    private Payment payment;

    public Order(String orderNumber, User user, OrderStatus status,
                 ShippingAddress shippingAddress,
                 BigDecimal subtotal, BigDecimal discount, BigDecimal totalAmount) {
        this.orderNumber = orderNumber;
        this.user = user;
        this.status = status;
        this.shippingAddress = shippingAddress;
        this.subtotal = subtotal;
        this.discount = discount;
        this.totalAmount = totalAmount;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void attachPayment(Payment payment) {
        this.payment = payment;
        payment.setOrder(this);
    }

    public boolean isCancellable() {
        return status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED;
    }
}
