package com.market.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.market.entity.Order;
import com.market.entity.OrderItem;
import java.util.List;

public interface OrderService extends IService<Order> {
    /** 从购物车生成订单（按摊主拆分） */
    List<Order> createOrdersFromCart(Long userId);
    /** 用户订单列表 */
    List<Order> listUserOrders(Long userId);
    /** 用户支付订单（模拟） */
    void payOrder(Long userId, Long orderId);
    /** 获取订单明细 */
    List<OrderItem> getOrderItems(Long orderId);
    /** 取消订单 */
    void cancelOrder(Long userId, Long orderId);
}