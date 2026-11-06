package com.market.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.market.entity.Order;
import com.market.entity.OrderItem;
import com.market.entity.Reservation;
import com.market.mapper.OrderMapper;
import com.market.mapper.OrderItemMapper;
import com.market.mapper.ProductMapper;
import com.market.mapper.ReservationMapper;
import com.market.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderScheduler {

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

    // 每 30 秒执行一次
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void cancelUnpaidOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(30);
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, "待付款")
                .le(Order::getCreateTime, deadline));

        for (Order order : orders) {
            // 支付或手动取消可能与定时任务并发；仅一次状态转换成功时才恢复库存。
            if (orderMapper.cancelIfExpired(order.getId(), deadline) != 1) continue;

            // 恢复库存时在数据库内原子递增，避免覆盖其他订单的扣减。
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
            for (OrderItem item : items) {
                productMapper.incrementStock(item.getProductId(), item.getQuantity());
            }

            // 3. 取消关联预定
            Reservation reservation = reservationMapper.selectOne(
                    new LambdaQueryWrapper<Reservation>()
                            .eq(Reservation::getOrderId, order.getId())
                            .eq(Reservation::getStatus, "已确认"));
            if (reservation != null) {
                reservation.setStatus("已取消");
                reservationMapper.updateById(reservation);
                // 通知预定用户
                notificationService.createNotification(
                        reservation.getUserId(),
                        "预定结果",
                        "您的预定已被取消（订单超时未支付）",
                        reservation.getId()
                );
            }

            // 4. 通知订单客户
            notificationService.createNotification(
                    order.getCustomerId(),
                    "订单状态",
                    "您的订单 " + order.getOrderNo() + " 因超时未支付已自动取消",
                    order.getId()
            );

            // 5. 通知摊主
            notificationService.createNotification(
                    order.getVendorId(),
                    "订单状态",
                    "订单 " + order.getOrderNo() + " 因用户超时未支付已自动取消",
                    order.getId()
            );
        }
    }
}
