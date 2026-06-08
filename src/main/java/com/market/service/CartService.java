package com.market.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.market.entity.Cart;
import java.util.List;

public interface CartService extends IService<Cart> {
    void addToCart(Long userId, Long productId, Integer quantity);
    List<Cart> listByUser(Long userId);
    void updateQuantity(Long userId, Long cartId, Integer quantity);
    void removeFromCart(Long userId, Long cartId);
    void clearCart(Long userId);
}