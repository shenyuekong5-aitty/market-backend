package com.market.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.market.entity.Order;
import com.market.entity.OrderItem;
import com.market.entity.Product;
import com.market.entity.Reservation;
import com.market.mapper.OrderMapper;
import com.market.mapper.OrderItemMapper;
import com.market.mapper.ProductMapper;
import com.market.mapper.ReservationMapper;
import com.market.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderScheduler {

    @Value("${order.cancel-timeout-minutes:30}")
    private int cancelTimeoutMinutes;

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ReservationMapper reservationMapper;
    @Autowired
    private NotificationService notificationService;

    // 每 30 秒执行一次（可根据需要调整）
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void cancelUnpaidOrders() {
        // 查询所有待付款且创建时间超过30分钟的订单
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(30);
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, "待付款")
                .le(Order::getCreateTime, deadline));

        for (Order order : orders) {
            // 1. 将订单状态改为已取消
            order.setStatus("已取消");
            orderMapper.updateById(order);

            // 2. 恢复库存
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
            for (OrderItem item : items) {
                Product product = productMapper.selectById(item.getProductId());
                if (product != null) {
                    product.setStock(product.getStock() + item.getQuantity());
                    productMapper.updateById(product);
                }
            }

            // 3. 取消关联的预定（如果有）
            Reservation reservation = reservationMapper.selectOne(
                    new LambdaQueryWrapper<Reservation>()
                            .eq(Reservation::getOrderId, order.getId())
                            .eq(Reservation::getStatus, "已确认"));
            if (reservation != null) {
                reservation.setStatus("已取消");
                reservationMapper.updateById(reservation);
                // 发送通知给预定用户
                notificationService.createNotification(
                        reservation.getUserId(),
                        "预定结果",
                        "您的预定已被取消（订单超时未支付）",
                        reservation.getId()
                );
            }

            // 4. 发送通知给订单客户
            notificationService.createNotification(
                    order.getCustomerId(),
                    "订单状态",
                    "您的订单 " + order.getOrderNo() + " 因超时未支付已自动取消",
                    order.getId()
            );
        }
    }
}