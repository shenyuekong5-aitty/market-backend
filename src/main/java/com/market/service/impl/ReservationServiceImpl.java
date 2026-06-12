package com.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.market.dto.ReservationDTO;
import com.market.entity.*;
import com.market.mapper.*;
import com.market.service.NotificationService;
import com.market.service.ReservationService;
import com.market.websocket.NotificationEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReservationServiceImpl extends ServiceImpl<ReservationMapper, Reservation> implements ReservationService {

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private BoothMapper boothMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private NotificationService notificationService;

    @Override
    @Transactional
    public void submitReservation(Long userId, Long productId, LocalDateTime startTime, LocalDateTime endTime) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getCanReserve() != 1 || !"上架".equals(product.getSaleStatus())) {
            throw new RuntimeException("商品不支持预定或已下架");
        }
        // 检查时间段冲突
        Long conflictCount = baseMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getProductId, productId)
                .in(Reservation::getStatus, "待确认", "已确认")
                .and(w -> w
                        .lt(Reservation::getStartTime, endTime)
                        .gt(Reservation::getEndTime, startTime)
                )
        );
        if (conflictCount > 0) {
            throw new RuntimeException("该时间段已被预定");
        }

        Booth booth = boothMapper.selectById(product.getBoothId());

        Reservation reservation = new Reservation();
        reservation.setProductId(productId);
        reservation.setUserId(userId);
        reservation.setStartTime(startTime);
        reservation.setEndTime(endTime);
        reservation.setVendorId(booth.getVendorId());
        reservation.setStatus("待确认");
        baseMapper.insert(reservation);

        // 数据库通知 + WebSocket 通知摊主
        notificationService.createNotification(
                reservation.getVendorId(),
                "预定请求",
                "用户 " + getUserName(userId) + " 预定了您的商品「" + product.getName() + "」，请及时处理",
                reservation.getId()
        );
        NotificationEndpoint.sendToUser(reservation.getVendorId().toString(), "new_notification");
    }

    @Override
    public List<ReservationDTO> listUserReservations(Long userId) {
        List<Reservation> list = baseMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, userId)
                .orderByDesc(Reservation::getCreateTime));
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<ReservationDTO> listVendorReservations(Long vendorId) {
        List<Reservation> list = baseMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getVendorId, vendorId)
                .orderByDesc(Reservation::getCreateTime));
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void confirmReservation(Long vendorId, Long reservationId) {
        Reservation reservation = baseMapper.selectById(reservationId);
        if (reservation == null || !"待确认".equals(reservation.getStatus())) {
            throw new RuntimeException("预定不存在或已处理");
        }
        if (!reservation.getVendorId().equals(vendorId)) {
            throw new RuntimeException("无权操作");
        }

        Product product = productMapper.selectById(reservation.getProductId());
        if (product.getStock() <= 0) {
            throw new RuntimeException("库存不足");
        }

        // 生成订单
        Booth booth = boothMapper.selectById(product.getBoothId());
        User vendor = userMapper.selectById(vendorId);
        User customer = userMapper.selectById(reservation.getUserId());
        String orderNo = "ORD" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                + String.format("%04d", new Random().nextInt(10000));

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setCustomerId(reservation.getUserId());
        order.setCustomerUsername(customer != null ? customer.getUsername() : "未知");
        order.setVendorId(vendorId);
        order.setVendorUsername(vendor != null ? vendor.getUsername() : "未知");
        order.setMarketId(booth.getMarketId());
        order.setBoothId(booth.getId());
        order.setTotalAmount(product.getPrice());
        order.setCreateTime(LocalDateTime.now());  // ✅ 使用应用服务器当前时间，避免时区问题导致订单立即被超时取消
        order.setStatus("待付款");
        orderMapper.insert(order);

        // 订单明细
        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setProductPrice(product.getPrice());
        item.setQuantity(1);
        item.setCreateTime(LocalDateTime.now());
        orderItemMapper.insert(item);

        // 扣减库存
        product.setStock(product.getStock() - 1);
        productMapper.updateById(product);

        // 更新预定状态
        reservation.setStatus("已确认");
        reservation.setOrderId(order.getId());
        baseMapper.updateById(reservation);

        // 数据库通知 + WebSocket 通知用户
        notificationService.createNotification(
                reservation.getUserId(),
                "预定结果",
                "您的商品「" + product.getName() + "」预定已被摊主确认，订单已生成，请尽快付款",
                reservation.getId()
        );
        NotificationEndpoint.sendToUser(reservation.getUserId().toString(), "new_notification");
    }

    @Override
    public void rejectReservation(Long vendorId, Long reservationId) {
        Reservation reservation = baseMapper.selectById(reservationId);
        if (reservation == null || !"待确认".equals(reservation.getStatus())) {
            throw new RuntimeException("预定不存在或已处理");
        }
        if (!reservation.getVendorId().equals(vendorId)) {
            throw new RuntimeException("无权操作");
        }
        reservation.setStatus("已拒绝");
        baseMapper.updateById(reservation);

        // 数据库通知 + WebSocket 通知用户
        Product product = productMapper.selectById(reservation.getProductId());
        notificationService.createNotification(
                reservation.getUserId(),
                "预定结果",
                "您的商品「" + (product != null ? product.getName() : "未知") + "」预定已被摊主拒绝",
                reservation.getId()
        );
        NotificationEndpoint.sendToUser(reservation.getUserId().toString(), "new_notification");
    }

    @Override
    public void cancelReservation(Long userId, Long reservationId) {
        Reservation reservation = baseMapper.selectById(reservationId);
        if (reservation == null || !reservation.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作");
        }
        if (!"待确认".equals(reservation.getStatus())) {
            throw new RuntimeException("当前状态不可取消");
        }
        reservation.setStatus("已取消");
        baseMapper.updateById(reservation);

        // 数据库通知 + WebSocket 通知摊主
        Product product = productMapper.selectById(reservation.getProductId());
        notificationService.createNotification(
                reservation.getVendorId(),
                "预定结果",
                "用户 " + getUserName(userId) + " 取消了商品「" + (product != null ? product.getName() : "未知") + "」的预定",
                reservation.getId()
        );
        NotificationEndpoint.sendToUser(reservation.getVendorId().toString(), "new_notification");
    }

    private ReservationDTO toDTO(Reservation r) {
        ReservationDTO dto = new ReservationDTO();
        dto.setId(r.getId());
        dto.setProductId(r.getProductId());
        dto.setUserId(r.getUserId());
        dto.setStartTime(r.getStartTime());
        dto.setEndTime(r.getEndTime());
        dto.setVendorId(r.getVendorId());
        dto.setStatus(r.getStatus());
        dto.setOrderId(r.getOrderId());
        dto.setCreateTime(r.getCreateTime());

        Product product = productMapper.selectById(r.getProductId());
        if (product != null) {
            dto.setProductName(product.getName());
            dto.setProductImageUrl(product.getImageUrl());
        }
        User user = userMapper.selectById(r.getUserId());
        if (user != null) {
            dto.setUserName(user.getNickname() != null ? user.getNickname() : user.getUsername());
        }
        return dto;
    }

    private String getUserName(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null ? (user.getNickname() != null ? user.getNickname() : user.getUsername()) : "未知";
    }
}