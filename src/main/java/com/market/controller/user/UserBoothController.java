package com.market.controller.user;

import com.market.common.Result;
import com.market.entity.Booth;
import com.market.entity.Product;
import com.market.service.BoothService;
import com.market.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserBoothController {

    @Autowired
    private BoothService boothService;

    @Autowired
    private ProductService productService;

    @GetMapping("/booths/{id}")
    public Result<Booth> getBooth(@PathVariable Long id) {
        Booth booth = boothService.getById(id);
        return Result.success(booth);
    }

    @GetMapping("/products")
    public Result<List<Product>> listProducts(@RequestParam Long boothId) {
        List<Product> list = productService.listByBoothId(boothId);  // 需要实现此方法
        return Result.success(list);
    }
}