package com.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.market.entity.Cart;
import com.market.entity.Product;
import com.market.mapper.CartMapper;
import com.market.mapper.ProductMapper;
import com.market.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {

    @Autowired
    private ProductMapper productMapper;

    @Override
    @Transactional
    public void addToCart(Long userId, Long productId, Integer quantity) {
        // 检查商品是否存在且上架
        Product product = productMapper.selectById(productId);
        if (product == null || !"上架".equals(product.getSaleStatus())) {
            throw new RuntimeException("商品不存在或已下架");
        }
        // 检查库存
        if (quantity > product.getStock()) {
            throw new RuntimeException("库存不足");
        }
        // 如果购物车已有该商品，则更新数量
        Cart exist = baseMapper.selectOne(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getProductId, productId));
        if (exist != null) {
            exist.setQuantity(exist.getQuantity() + quantity);
            baseMapper.updateById(exist);
        } else {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(productId);
            cart.setQuantity(quantity);
            baseMapper.insert(cart);
        }
    }

    @Override
    public List<Cart> listByUser(Long userId) {
        return baseMapper.selectList(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId));
    }

    @Override
    @Transactional
    public void updateQuantity(Long userId, Long cartId, Integer quantity) {
        Cart cart = baseMapper.selectById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作");
        }
        Product product = productMapper.selectById(cart.getProductId());
        if (quantity > product.getStock()) {
            throw new RuntimeException("库存不足");
        }
        cart.setQuantity(quantity);
        baseMapper.updateById(cart);
    }

    @Override
    @Transactional
    public void removeFromCart(Long userId, Long cartId) {
        Cart cart = baseMapper.selectById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作");
        }
        baseMapper.deleteById(cartId);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        baseMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
    }
}