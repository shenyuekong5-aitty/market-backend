package com.market.controller.vendor;

import com.market.common.Result;
import com.market.entity.Product;
import com.market.entity.User;
import com.market.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendor/products")
public class VendorProductController {

    @Autowired
    private ProductService productService;

    // 获取当前小贩的所有商品
    @GetMapping
    public Result<List<Product>> list() {
        Long vendorId = getCurrentUserId();
        return Result.success(productService.listByVendor(vendorId));
    }

    // 新增商品
    @PostMapping
    public Result<Product> add(@RequestBody Product product) {
        Long vendorId = getCurrentUserId();
        return Result.success(productService.addProduct(vendorId, product));
    }

    // 更新商品
    @PutMapping("/{id}")
    public Result<Product> update(@PathVariable Long id, @RequestBody Product product) {
        product.setId(id);
        Long vendorId = getCurrentUserId();
        return Result.success(productService.updateProduct(vendorId, product));
    }

    // 删除商品
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        Long vendorId = getCurrentUserId();
        productService.deleteProduct(vendorId, id);
        return Result.success("商品已删除");
    }

    // 上架/下架
    @PutMapping("/{id}/toggle-status")
    public Result<String> toggleStatus(@PathVariable Long id) {
        Long vendorId = getCurrentUserId();
        productService.toggleSaleStatus(vendorId, id);
        return Result.success("状态已更新");
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return ((User) principal).getId();
        }
        throw new RuntimeException("用户未登录");
    }
}