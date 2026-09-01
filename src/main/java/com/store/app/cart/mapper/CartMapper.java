package com.store.app.cart.mapper;

import com.store.app.cart.dto.CartItemResponse;
import com.store.app.cart.dto.CartResponse;
import com.store.app.cart.entity.Cart;
import com.store.app.cart.entity.CartItem;
import com.store.app.product.entity.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Maps carts to DTOs and computes the money figures:
 * subtotal (regular prices) − discount = total (current selling prices).
 */
@Component
public class CartMapper {

    public CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        int totalItems = 0;

        for (CartItem item : cart.getItems()) {
            BigDecimal quantity = BigDecimal.valueOf(item.getQuantity());
            subtotal = subtotal.add(item.getProduct().getPrice().multiply(quantity));
            total = total.add(effectivePrice(item.getProduct()).multiply(quantity));
            totalItems += item.getQuantity();
        }

        return new CartResponse(
                cart.getId(),
                items,
                totalItems,
                subtotal,
                subtotal.subtract(total),
                total
        );
    }

    public CartItemResponse toItemResponse(CartItem item) {
        Product product = item.getProduct();
        BigDecimal effective = effectivePrice(product);
        String imageUrl = product.getImages().isEmpty()
                ? null
                : product.getImages().get(0).getImageUrl();

        return new CartItemResponse(
                item.getId(),
                product.getId(),
                product.getProductName(),
                product.getSlug(),
                imageUrl,
                product.getPrice(),
                effective,
                item.getPriceAtAddition(),
                effective.compareTo(item.getPriceAtAddition()) != 0,
                item.getQuantity(),
                product.getStockQuantity(),
                effective.multiply(BigDecimal.valueOf(item.getQuantity()))
        );
    }

    /** Current selling price: the discount price when set, else the price. */
    public BigDecimal effectivePrice(Product product) {
        return product.getDiscountPrice() != null
                ? product.getDiscountPrice()
                : product.getPrice();
    }
}
