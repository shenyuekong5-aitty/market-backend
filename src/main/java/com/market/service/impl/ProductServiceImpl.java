package com.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.market.entity.Booth;
import com.market.entity.Cart;
import com.market.entity.Product;
import com.market.mapper.BoothMapper;
import com.market.mapper.CartMapper;
import com.market.mapper.ProductMapper;
import com.market.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Autowired
    private BoothMapper boothMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private JdbcTemplate jdbc;

    @Override
    public List<Product> listByVendor(Long vendorId) {
        return baseMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getVendorId, vendorId));
    }

    @Override
    @Transactional
    public Product addProduct(Long vendorId, Product product) {
        validateProduct(product);
        Booth booth = boothMapper.selectById(product.getBoothId());
        if (booth == null || !vendorId.equals(booth.getVendorId()) || !"已占用".equals(booth.getStatus())) {
            throw new RuntimeException("无权在该摊位下添加商品");
        }
        product.setVendorId(vendorId);
        product.setSaleStatus("上架");
        baseMapper.insert(product);
        return product;
    }

    @Override
    @Transactional
    public Product updateProduct(Long vendorId, Product product) {
        validateProduct(product);
        lockProduct(product.getId());
        Product existing = baseMapper.selectById(product.getId());
        if (existing == null || !vendorId.equals(existing.getVendorId())) {
            throw new RuntimeException("无权修改该商品");
        }
        existing.setName(product.getName());
        existing.setPrice(product.getPrice());
        existing.setCanReserve(product.getCanReserve());
        existing.setStock(product.getStock());
        existing.setImageUrl(product.getImageUrl());
        baseMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional
    public void deleteProduct(Long vendorId, Long productId) {
        lockProduct(productId);
        Product product = baseMapper.selectById(productId);
        if (product == null || !vendorId.equals(product.getVendorId())) {
            throw new RuntimeException("无权删除该商品");
        }
        Long pendingReservations = jdbc.queryForObject(
                "SELECT COUNT(*) FROM reservation WHERE product_id = ? AND status IN ('待确认', '已确认')",
                Long.class, productId);
        Long activeOrders = jdbc.queryForObject("""
                SELECT COUNT(*) FROM order_item oi JOIN order_master o ON o.id = oi.order_id
                WHERE oi.product_id = ? AND o.status IN ('待付款', '已付款', '待取货')
                """, Long.class, productId);
        if ((pendingReservations != null && pendingReservations > 0) || (activeOrders != null && activeOrders > 0)) {
            throw new RuntimeException("商品还有未处理的预订或订单，暂不能删除；请先下架并处理相关记录");
        }
        // 商品删除后购物车不能留下无法结算的记录。
        cartMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getProductId, productId));
        baseMapper.deleteById(productId);
    }

    @Override
    @Transactional
    public void toggleSaleStatus(Long vendorId, Long productId) {
        lockProduct(productId);
        Product product = baseMapper.selectById(productId);
        if (product == null || !vendorId.equals(product.getVendorId())) {
            throw new RuntimeException("无权操作该商品");
        }
        if (!"上架".equals(product.getSaleStatus())) {
            Booth booth = boothMapper.selectById(product.getBoothId());
            if (booth == null || !"已占用".equals(booth.getStatus()) || !vendorId.equals(booth.getVendorId())) {
                throw new RuntimeException("摊位当前不可上架商品");
            }
        }
        product.setSaleStatus("上架".equals(product.getSaleStatus()) ? "下架" : "上架");
        baseMapper.updateById(product);
    }

    @Override
    public List<Product> listByBoothId(Long boothId) {
        return baseMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getBoothId, boothId)
                .eq(Product::getSaleStatus, "上架"));
    }

    private void validateProduct(Product product) {
        if (product == null || product.getName() == null || product.getName().isBlank()
                || product.getName().length() > 100) {
            throw new IllegalArgumentException("商品名称应为 1～100 个字符");
        }
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("商品价格不能为负数");
        }
        if (product.getStock() == null || product.getStock() < 0) {
            throw new IllegalArgumentException("商品库存不能为负数");
        }
        if (product.getCanReserve() == null || (product.getCanReserve() != 0 && product.getCanReserve() != 1)) {
            throw new IllegalArgumentException("预订选项无效");
        }
    }

    private void lockProduct(Long productId) {
        if (productId == null || baseMapper.lockId(productId) == null) {
            throw new RuntimeException("商品不存在");
        }
    }
}
