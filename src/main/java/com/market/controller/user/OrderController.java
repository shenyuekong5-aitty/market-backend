package com.market.controller.user;

import com.market.common.Result;
import com.market.entity.Order;
import com.market.entity.OrderItem;
import com.market.entity.User;
import com.market.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // 从购物车生成订单
    @PostMapping("/createFromCart")
    public Result<List<Order>> createFromCart() {
        Long userId = getCurrentUserId();
        List<Order> orders = orderService.createOrdersFromCart(userId);
        return Result.success(orders);
    }

    // 订单列表
    @GetMapping
    public Result<List<Order>> list() {
        Long userId = getCurrentUserId();
        return Result.success(orderService.listUserOrders(userId));
    }

    // 订单明细
    @GetMapping("/{orderId}/items")
    public Result<List<OrderItem>> getItems(@PathVariable Long orderId) {
        return Result.success(orderService.getOrderItems(orderId));
    }

    // 支付订单
    @PutMapping("/{orderId}/pay")
    public Result<String> pay(@PathVariable Long orderId) {
        Long userId = getCurrentUserId();
        orderService.payOrder(userId, orderId);
        return Result.success("支付成功");
    }

    // 取消订单
    @PutMapping("/{orderId}/cancel")
    public Result<String> cancel(@PathVariable Long orderId) {
        Long userId = getCurrentUserId();
        orderService.cancelOrder(userId, orderId);
        return Result.success("订单已取消");
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return ((User) principal).getId();
        }
        throw new RuntimeException("用户未登录");
    }
}