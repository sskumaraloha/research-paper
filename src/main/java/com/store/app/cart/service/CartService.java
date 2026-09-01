package com.store.app.cart.service;

import com.store.app.cart.dto.AddToCartRequest;
import com.store.app.cart.dto.CartResponse;

/**
 * Shopping cart operations. Carting never changes inventory — stock is
 * only validated here and actually reduced during order placement.
 */
public interface CartService {

    /** The user's cart (created on first use), with computed totals. */
    CartResponse getCart(Long userId);

    /**
     * Adds a product (or increases its line) in the user's cart.
     *
     * @throws com.store.app.exception.ResourceNotFoundException
     *         if the product does not exist or is inactive
     * @throws com.store.app.exception.BusinessValidationException
     *         if the resulting quantity would exceed available stock
     */
    CartResponse addToCart(Long userId, AddToCartRequest request);

    /** Sets an item's quantity to an absolute value (stock-validated). */
    CartResponse updateItemQuantity(Long userId, Long itemId, int quantity);

    /** Increases an item's quantity by one (stock-validated). */
    CartResponse increaseItemQuantity(Long userId, Long itemId);

    /** Decreases an item's quantity by one; at one, the item is removed. */
    CartResponse decreaseItemQuantity(Long userId, Long itemId);

    CartResponse removeItem(Long userId, Long itemId);

    CartResponse clearCart(Long userId);

    /** Total units in the user's cart (navbar badge). */
    int countItems(Long userId);

    /** Removes a product from every cart (called when a product is deleted). */
    void removeProductFromCarts(Long productId);
}
