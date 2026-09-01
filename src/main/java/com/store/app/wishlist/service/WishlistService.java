package com.store.app.wishlist.service;

import com.store.app.wishlist.dto.WishlistItemResponse;

import java.util.List;

/**
 * Wishlist operations. All methods are scoped to the acting user;
 * items of other users are unreachable by construction.
 */
public interface WishlistService {

    List<WishlistItemResponse> getWishlist(Long userId);

    /**
     * Adds a product to the user's wishlist.
     *
     * @throws com.store.app.exception.ResourceNotFoundException
     *         if the product does not exist or is inactive
     * @throws com.store.app.exception.DuplicateResourceException
     *         if the product is already on the wishlist
     */
    WishlistItemResponse addProduct(Long userId, Long productId);

    void removeItem(Long userId, Long itemId);

    /**
     * Moves a wishlist item into the cart (quantity 1) and removes it
     * from the wishlist. Cart stock validation applies; when it fails,
     * the item stays on the wishlist.
     */
    void moveToCart(Long userId, Long itemId);

    /** Removes a product from every wishlist (product deletion). */
    void removeProductFromWishlists(Long productId);
}
