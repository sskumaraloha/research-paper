package com.store.app.wishlist.service.impl;

import com.store.app.cart.dto.AddToCartRequest;
import com.store.app.cart.service.CartService;
import com.store.app.exception.DuplicateResourceException;
import com.store.app.exception.ResourceNotFoundException;
import com.store.app.product.entity.Product;
import com.store.app.product.mapper.ProductMapper;
import com.store.app.product.repository.ProductRepository;
import com.store.app.user.entity.User;
import com.store.app.user.repository.UserRepository;
import com.store.app.wishlist.dto.WishlistItemResponse;
import com.store.app.wishlist.entity.WishlistItem;
import com.store.app.wishlist.repository.WishlistItemRepository;
import com.store.app.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistServiceImpl implements WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;
    private final CartService cartService;

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getWishlist(Long userId) {
        return wishlistItemRepository.findAllByUserIdOrderByIdDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public WishlistItemResponse addProduct(Long userId, Long productId) {
        Product product = productRepository.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + productId));

        if (wishlistItemRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new DuplicateResourceException(
                    "\"" + product.getProductName() + "\" is already on your wishlist");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId));

        return toResponse(wishlistItemRepository.save(new WishlistItem(user, product)));
    }

    @Override
    public void removeItem(Long userId, Long itemId) {
        wishlistItemRepository.delete(requiredItem(userId, itemId));
    }

    @Override
    public void moveToCart(Long userId, Long itemId) {
        WishlistItem item = requiredItem(userId, itemId);

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(item.getProduct().getId());
        request.setQuantity(1);
        // Throws on stock problems, leaving the wishlist item in place.
        cartService.addToCart(userId, request);

        wishlistItemRepository.delete(item);
    }

    @Override
    public void removeProductFromWishlists(Long productId) {
        wishlistItemRepository.deleteAllByProductId(productId);
    }

    // ------------------------------------------------------------------

    private WishlistItem requiredItem(Long userId, Long itemId) {
        return wishlistItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Wishlist item not found with id: " + itemId));
    }

    private WishlistItemResponse toResponse(WishlistItem item) {
        return new WishlistItemResponse(
                item.getId(),
                productMapper.toCard(item.getProduct()),
                item.getCreatedAt()
        );
    }
}
