package com.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.market.dto.CartItemDTO;
import com.market.entity.Cart;
import com.market.entity.Product;
import com.market.mapper.CartMapper;
import com.market.mapper.ProductMapper;
import com.market.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {

    @Autowired
    private ProductMapper productMapper;

    @Override
    @Transactional
    public void addToCart(Long userId, Long productId, Integer quantity) {
        Product product = productMapper.selectById(productId);
        if (product == null || !"上架".equals(product.getSaleStatus())) {
            throw new RuntimeException("商品不存在或已下架");
        }
        if (quantity > product.getStock()) {
            throw new RuntimeException("库存不足");
        }

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

    @Override
    public List<CartItemDTO> listUserCart(Long userId) {
        List<Cart> cartItems = baseMapper.selectList(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId));

        return cartItems.stream().map(cart -> {
            Product product = productMapper.selectById(cart.getProductId());
            CartItemDTO dto = new CartItemDTO();
            dto.setCartId(cart.getId());
            dto.setProductId(cart.getProductId());
            dto.setQuantity(cart.getQuantity());
            if (product != null) {
                dto.setProductName(product.getName());
                dto.setProductPrice(product.getPrice());
                dto.setProductImageUrl(product.getImageUrl());
                dto.setStock(product.getStock());
            }
            return dto;
        }).collect(Collectors.toList());
    }
}