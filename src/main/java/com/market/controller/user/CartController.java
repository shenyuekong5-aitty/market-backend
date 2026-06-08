package com.market.controller.user;

import com.market.common.Result;
import com.market.entity.Cart;
import com.market.entity.User;
import com.market.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    // 添加到购物车
    @PostMapping("/add")
    public Result<String> add(@RequestParam Long productId, @RequestParam(defaultValue = "1") Integer quantity) {
        Long userId = getCurrentUserId();
        cartService.addToCart(userId, productId, quantity);
        return Result.success("已加入购物车");
    }

    // 查看购物车列表
    @GetMapping
    public Result<List<Cart>> list() {
        Long userId = getCurrentUserId();
        return Result.success(cartService.listByUser(userId));
    }

    // 修改数量
    @PutMapping("/{cartId}/quantity")
    public Result<String> updateQuantity(@PathVariable Long cartId, @RequestParam Integer quantity) {
        Long userId = getCurrentUserId();
        cartService.updateQuantity(userId, cartId, quantity);
        return Result.success("数量已更新");
    }

    // 删除商品
    @DeleteMapping("/{cartId}")
    public Result<String> remove(@PathVariable Long cartId) {
        Long userId = getCurrentUserId();
        cartService.removeFromCart(userId, cartId);
        return Result.success("已移出购物车");
    }

    // 清空购物车
    @DeleteMapping("/clear")
    public Result<String> clear() {
        Long userId = getCurrentUserId();
        cartService.clearCart(userId);
        return Result.success("购物车已清空");
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return ((User) principal).getId();
        }
        throw new RuntimeException("用户未登录");
    }
}