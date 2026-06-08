package com.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.market.entity.Booth;
import com.market.entity.Product;
import com.market.mapper.BoothMapper;
import com.market.mapper.ProductMapper;
import com.market.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Autowired
    private BoothMapper boothMapper;

    @Override
    public List<Product> listByVendor(Long vendorId) {
        return baseMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getVendorId, vendorId));
    }

    @Override
    @Transactional
    public Product addProduct(Long vendorId, Product product) {
        Booth booth = boothMapper.selectById(product.getBoothId());
        if (booth == null || !booth.getVendorId().equals(vendorId)) {
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
        Product existing = baseMapper.selectById(product.getId());
        if (existing == null || !existing.getVendorId().equals(vendorId)) {
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
        Product product = baseMapper.selectById(productId);
        if (product == null || !product.getVendorId().equals(vendorId)) {
            throw new RuntimeException("无权删除该商品");
        }
        baseMapper.deleteById(productId);
    }

    @Override
    @Transactional
    public void toggleSaleStatus(Long vendorId, Long productId) {
        Product product = baseMapper.selectById(productId);
        if (product == null || !product.getVendorId().equals(vendorId)) {
            throw new RuntimeException("无权操作该商品");
        }
        product.setSaleStatus("上架".equals(product.getSaleStatus()) ? "下架" : "上架");
        baseMapper.updateById(product);
    }
}