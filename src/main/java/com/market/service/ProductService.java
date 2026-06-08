package com.market.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.market.entity.Product;
import java.util.List;

public interface ProductService extends IService<Product> {
    List<Product> listByVendor(Long vendorId);
    Product addProduct(Long vendorId, Product product);
    Product updateProduct(Long vendorId, Product product);
    void deleteProduct(Long vendorId, Long productId);
    void toggleSaleStatus(Long vendorId, Long productId);
}