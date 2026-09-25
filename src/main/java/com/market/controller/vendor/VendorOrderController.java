package com.market.controller.vendor;

import com.market.common.Result;
import com.market.entity.Order;
import com.market.entity.OrderItem;
import com.market.entity.User;
import com.market.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("vendorOrderController")
@RequestMapping("/api/vendor/orders")
public class VendorOrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping
    public Result<List<Order>> list() {
        Long vendorId = getCurrentVendorId();
        return Result.success(orderService.listVendorOrders(vendorId));
    }

    // 获取当前摊主订单明细
    @GetMapping("/{orderId}/items")
    public Result<List<OrderItem>> getItems(@PathVariable Long orderId) {
        Long vendorId = getCurrentVendorId();
        Order order = orderService.getById(orderId);
        if (order == null || !vendorId.equals(order.getVendorId())) {
            throw new RuntimeException("订单不存在或无权查看");
        }
        return Result.success(orderService.getOrderItems(orderId));
    }

    private Long getCurrentVendorId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return ((User) principal).getId();
        }
        throw new RuntimeException("用户未登录");
    }
}
