package com.store.app.cart.service.impl;

import com.store.app.cart.dto.AddToCartRequest;
import com.store.app.cart.dto.CartResponse;
import com.store.app.cart.entity.Cart;
import com.store.app.cart.entity.CartItem;
import com.store.app.cart.mapper.CartMapper;
import com.store.app.cart.repository.CartItemRepository;
import com.store.app.cart.repository.CartRepository;
import com.store.app.cart.service.CartService;
import com.store.app.exception.BusinessValidationException;
import com.store.app.exception.ResourceNotFoundException;
import com.store.app.product.entity.Product;
import com.store.app.product.repository.ProductRepository;
import com.store.app.user.entity.User;
import com.store.app.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;

    @Override
    public CartResponse getCart(Long userId) {
        return cartMapper.toResponse(getOrCreateCart(userId));
    }

    @Override
    public CartResponse addToCart(Long userId, AddToCartRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + request.getProductId()));

        Cart cart = getOrCreateCart(userId);
        CartItem existing = cart.findItemByProductId(product.getId()).orElse(null);
        int requestedTotal = request.getQuantity()
                + (existing == null ? 0 : existing.getQuantity());
        assertWithinStock(product, requestedTotal, existing != null);

        if (existing == null) {
            cart.addItem(new CartItem(
                    product, request.getQuantity(), cartMapper.effectivePrice(product)));
        } else {
            existing.setQuantity(requestedTotal);
        }

        return cartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    public CartResponse updateItemQuantity(Long userId, Long itemId, int quantity) {
        if (quantity < 1) {
            throw new BusinessValidationException("Quantity must be at least 1");
        }
        Cart cart = getOrCreateCart(userId);
        CartItem item = requiredItem(cart, itemId);
        assertWithinStock(item.getProduct(), quantity, false);
        item.setQuantity(quantity);
        return cartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    public CartResponse increaseItemQuantity(Long userId, Long itemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = requiredItem(cart, itemId);
        assertWithinStock(item.getProduct(), item.getQuantity() + 1, true);
        item.setQuantity(item.getQuantity() + 1);
        return cartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    public CartResponse decreaseItemQuantity(Long userId, Long itemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = requiredItem(cart, itemId);
        if (item.getQuantity() <= 1) {
            cart.removeItem(item);
        } else {
            item.setQuantity(item.getQuantity() - 1);
        }
        return cartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    public CartResponse removeItem(Long userId, Long itemId) {
        Cart cart = getOrCreateCart(userId);
        cart.removeItem(requiredItem(cart, itemId));
        return cartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    public CartResponse clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.clear();
        return cartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional(readOnly = true)
    public int countItems(Long userId) {
        return cartItemRepository.countUnitsByUserId(userId);
    }

    @Override
    public void removeProductFromCarts(Long productId) {
        cartItemRepository.deleteAllByProductId(productId);
    }

    // ------------------------------------------------------------------

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User not found with id: " + userId));
            return cartRepository.save(new Cart(user));
        });
    }

    private CartItem requiredItem(Cart cart, Long itemId) {
        return cart.findItemById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found with id: " + itemId));
    }

    /**
     * Cart-time stock check against the live quantity. Inventory itself
     * is untouched here; the authoritative reservation happens when the
     * order is placed.
     */
    private void assertWithinStock(Product product, int requestedQuantity,
                                   boolean mentionCart) {
        int available = product.getStockQuantity();
        if (available <= 0) {
            throw new BusinessValidationException(
                    "\"" + product.getProductName() + "\" is out of stock");
        }
        if (requestedQuantity > available) {
            String suffix = mentionCart ? " (including what is already in your cart)" : "";
            throw new BusinessValidationException(
                    "Only " + available + " unit(s) of \"" + product.getProductName()
                            + "\" available" + suffix);
        }
    }
}
